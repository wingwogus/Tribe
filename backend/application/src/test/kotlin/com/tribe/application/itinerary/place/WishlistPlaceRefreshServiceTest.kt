package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.place.PlaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDateTime

class WishlistPlaceRefreshServiceTest {
    private lateinit var placeRepository: PlaceRepository
    private lateinit var placeCatalogService: PlaceCatalogService
    private lateinit var sleeper: RecordingSleeper
    private lateinit var service: WishlistPlaceRefreshService

    @BeforeEach
    fun setUp() {
        placeRepository = mockk()
        placeCatalogService = mockk()
        sleeper = RecordingSleeper()
        service = WishlistPlaceRefreshService(placeRepository, placeCatalogService, sleeper)
    }

    @Test
    fun `refreshActiveWishlistPlaces refreshes stale active places and skips fresh ones`() {
        val stale = place("stale", syncedAt = LocalDateTime.now().minusDays(40))
        val fresh = place("fresh", syncedAt = LocalDateTime.now())
        stubRefreshCandidates(batchSize = 10, places = listOf(stale, fresh))
        every { placeCatalogService.refreshDetailsById(stale.id, "ko") } returns true

        val summary = service.refreshActiveWishlistPlaces(
            WishlistPlaceRefreshRequest(batchSize = 10, rateLimitPerMinute = 600),
        )

        assertEquals(2, summary.scanned)
        assertEquals(1, summary.processed)
        assertEquals(1, summary.skipped)
        assertEquals(1, summary.succeeded)
        assertEquals(0, summary.failed)
        verify { placeCatalogService.refreshDetailsById(stale.id, "ko") }
        verify(exactly = 0) { placeCatalogService.refreshDetailsById(fresh.id, "ko") }
    }

    @Test
    fun `refreshActiveWishlistPlaces retries transient failures and rate limits repeated calls`() {
        val stale = place("retry", syncedAt = LocalDateTime.now().minusDays(40))
        stubRefreshCandidates(batchSize = 1, places = listOf(stale))
        every { placeCatalogService.refreshDetailsById(stale.id, "ko") } throws
            googleResponseException(HttpStatus.INTERNAL_SERVER_ERROR) andThen true

        val summary = service.refreshActiveWishlistPlaces(
            WishlistPlaceRefreshRequest(
                batchSize = 1,
                rateLimitPerMinute = 120,
                maxAttempts = 2,
                retryBackoffMillis = 25,
            ),
        )

        assertEquals(1, summary.succeeded)
        assertEquals(0, summary.failed)
        assertEquals(1, summary.retries)
        assertEquals(listOf(25L, 500L), sleeper.sleeps)
        verify(exactly = 2) { placeCatalogService.refreshDetailsById(stale.id, "ko") }
    }

    @Test
    fun `refreshActiveWishlistPlaces does not retry non-transient failures`() {
        val stale = place("fail", syncedAt = LocalDateTime.now().minusDays(40))
        val failure = googleResponseException(HttpStatus.FORBIDDEN)
        stubRefreshCandidates(batchSize = 1, places = listOf(stale))
        every { placeCatalogService.refreshDetailsById(stale.id, "ko") } throws failure

        val summary = service.refreshActiveWishlistPlaces(
            WishlistPlaceRefreshRequest(batchSize = 1, maxAttempts = 3, retryBackoffMillis = 10),
        )

        assertEquals(0, summary.succeeded)
        assertEquals(1, summary.failed)
        assertEquals(0, summary.retries)
        assertEquals(mapOf((failure::class.simpleName ?: "exception") to 1), summary.failureReasons)
        assertEquals(emptyList<Long>(), sleeper.sleeps)
        verify(exactly = 1) { placeCatalogService.refreshDetailsById(stale.id, "ko") }
    }

    @Test
    fun `refreshActiveWishlistPlaces records failure reason after transient retry exhaustion`() {
        val stale = place("exhausted", syncedAt = LocalDateTime.now().minusDays(40))
        val failure = googleResponseException(HttpStatus.INTERNAL_SERVER_ERROR)
        stubRefreshCandidates(batchSize = 1, places = listOf(stale))
        every { placeCatalogService.refreshDetailsById(stale.id, "ko") } throws failure

        val summary = service.refreshActiveWishlistPlaces(
            WishlistPlaceRefreshRequest(
                batchSize = 1,
                rateLimitPerMinute = 600,
                maxAttempts = 2,
                retryBackoffMillis = 10,
            ),
        )

        assertEquals(0, summary.succeeded)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.retries)
        assertEquals(mapOf((failure::class.simpleName ?: "exception") to 1), summary.failureReasons)
        assertEquals(listOf(10L, 100L), sleeper.sleeps)
    }

    @Test
    fun `refreshActiveWishlistPlaces counts empty details as failed without retrying`() {
        val stale = place("empty", syncedAt = LocalDateTime.now().minusDays(40))
        stubRefreshCandidates(batchSize = 1, places = listOf(stale))
        every { placeCatalogService.refreshDetailsById(stale.id, "ko") } returns false

        val summary = service.refreshActiveWishlistPlaces(WishlistPlaceRefreshRequest(batchSize = 1))

        assertEquals(1, summary.failed)
        assertEquals(0, summary.retries)
        assertEquals(mapOf("empty_details" to 1), summary.failureReasons)
    }

    private fun stubRefreshCandidates(
        batchSize: Int,
        places: List<Place>,
    ) {
        every {
            placeRepository.findActiveWishlistedPlacesForRefresh(
                any(),
                any(),
                any(),
                PageRequest.of(0, batchSize),
            )
        } returns places
    }

    private fun googleResponseException(status: HttpStatus): WebClientResponseException =
        WebClientResponseException.create(
            status.value(),
            status.reasonPhrase,
            HttpHeaders.EMPTY,
            ByteArray(0),
            null as Charset?,
        )

    private fun place(
        suffix: String,
        syncedAt: LocalDateTime?,
    ): Place {
        val place = Place(
            externalPlaceId = "place-$suffix",
            name = "Place $suffix",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )
        ReflectionTestUtils.setField(place, "id", suffix.hashCode().toLong())
        place.detailsSyncedAt = syncedAt
        place.detailSnapshot = PlaceDetailSnapshot(
            place = place,
            detailsSyncedAt = syncedAt,
            openingHoursSyncedAt = syncedAt,
            currentOpeningHoursSyncedAt = syncedAt,
        )
        return place
    }

    private class RecordingSleeper : WishlistPlaceRefreshSleeper {
        val sleeps: MutableList<Long> = mutableListOf()

        override fun sleep(millis: Long) {
            sleeps.add(millis)
        }
    }
}
