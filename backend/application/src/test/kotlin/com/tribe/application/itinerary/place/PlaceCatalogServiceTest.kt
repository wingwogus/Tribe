package com.tribe.application.itinerary.place

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.place.PlaceDetailSnapshotRepository
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriodRepository
import com.tribe.domain.itinerary.place.PlaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.SimpleTransactionStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PlaceCatalogServiceTest {
    @Mock private lateinit var placeResultAssembler: PlaceResultAssembler
    @Mock private lateinit var placeRepository: PlaceRepository
    @Mock private lateinit var detailSnapshotRepository: PlaceDetailSnapshotRepository
    @Mock private lateinit var openingPeriodRepository: PlaceRegularOpeningPeriodRepository
    @Mock private lateinit var placeSearchGateway: PlaceSearchGateway
    @Mock private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var service: PlaceCatalogService

    @BeforeEach
    fun setUp() {
        service = PlaceCatalogService(
            objectMapper = jacksonObjectMapper(),
            placeResultAssembler = placeResultAssembler,
            placeRepository = placeRepository,
            detailSnapshotRepository = detailSnapshotRepository,
            openingPeriodRepository = openingPeriodRepository,
            placeSearchGateway = placeSearchGateway,
            transactionManager = transactionManager,
        )
    }

    @Test
    fun `getOrCreateAndEnrich reuses concurrently created place when save hits unique constraint`() {
        `when`(transactionManager.getTransaction(any(TransactionDefinition::class.java)))
            .thenReturn(SimpleTransactionStatus())
        val concurrentPlace = Place("place-1", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(concurrentPlace, "id", 10L)
        concurrentPlace.detailsSyncedAt = LocalDateTime.now()
        concurrentPlace.detailSnapshot = PlaceDetailSnapshot(
            place = concurrentPlace,
            openingHoursSyncedAt = LocalDateTime.now(),
            currentOpeningHoursSyncedAt = LocalDateTime.now(),
        )

        `when`(placeRepository.findByExternalPlaceId("place-1")).thenReturn(null, concurrentPlace)
        `when`(placeRepository.saveAndFlush(any(Place::class.java)))
            .thenThrow(DataIntegrityViolationException("duplicate place"))

        val result = service.getOrCreateAndEnrich(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )

        assertEquals(10L, result.id)
        assertEquals("place-1", result.externalPlaceId)
        verifyNoInteractions(placeSearchGateway)
    }

    @Test
    fun `getOrCreateAndEnrich stores split opening timestamps photo and refreshed periods`() {
        val place = Place("place-1", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 10L)
        val details = PlaceSearchGateway.DetailsPayload(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = 1.0,
            longitude = 10.0,
            primaryPhotoName = "places/place-1/photos/photo-1",
            regularOpeningHoursJson = """{"periods":[]}""",
            currentOpeningHoursJson = """{"openNow":true}""",
            regularOpeningPeriods = listOf(
                PlaceSearchGateway.RegularOpeningPeriodInput(
                    dayOfWeek = 0,
                    openMinute = 9 * 60,
                    closeMinute = 18 * 60,
                    isOvernight = false,
                    sequenceNo = 1,
                ),
            ),
        )

        `when`(placeRepository.findByExternalPlaceId("place-1")).thenReturn(place)
        `when`(placeSearchGateway.getPlaceDetails("place-1", "ko")).thenReturn(details)
        `when`(detailSnapshotRepository.findById(10L)).thenReturn(Optional.empty())
        `when`(detailSnapshotRepository.save(any(PlaceDetailSnapshot::class.java))).thenAnswer { it.arguments[0] }

        val result = service.getOrCreateAndEnrich(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )

        assertEquals("places/place-1/photos/photo-1", result.detailSnapshot?.primaryPhotoName)
        assertNotNull(result.detailSnapshot?.openingHoursSyncedAt)
        assertNotNull(result.detailSnapshot?.currentOpeningHoursSyncedAt)
        assertEquals(1, result.regularOpeningPeriods.size)
        assertEquals(0, result.regularOpeningPeriods.first().dayOfWeek)
        verify(openingPeriodRepository).deleteAllByPlaceId(10L)
    }

    @Test
    fun `getOrCreateAndEnrich refreshes old detail rows missing split timestamps`() {
        val place = Place("place-1", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 10L)
        place.detailsSyncedAt = LocalDateTime.now()
        val snapshot = PlaceDetailSnapshot(place = place, detailsSyncedAt = place.detailsSyncedAt)
        place.detailSnapshot = snapshot
        val details = PlaceSearchGateway.DetailsPayload(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = 1.0,
            longitude = 10.0,
        )

        `when`(placeRepository.findByExternalPlaceId("place-1")).thenReturn(place)
        `when`(placeSearchGateway.getPlaceDetails("place-1", "ko")).thenReturn(details)
        `when`(detailSnapshotRepository.findById(10L)).thenReturn(Optional.of(snapshot))
        `when`(detailSnapshotRepository.save(any(PlaceDetailSnapshot::class.java))).thenAnswer { it.arguments[0] }

        service.getOrCreateAndEnrich(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )

        verify(placeSearchGateway).getPlaceDetails("place-1", "ko")
        assertNotNull(place.detailSnapshot?.openingHoursSyncedAt)
        assertNotNull(place.detailSnapshot?.currentOpeningHoursSyncedAt)
    }

    @Test
    fun `refreshDetailsById refreshes an existing place for scheduled jobs`() {
        val place = Place("place-1", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 10L)
        val details = PlaceSearchGateway.DetailsPayload(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = 1.0,
            longitude = 10.0,
            primaryPhotoName = "places/place-1/photos/photo-1",
        )

        `when`(placeRepository.findById(10L)).thenReturn(Optional.of(place))
        `when`(placeSearchGateway.getPlaceDetails("place-1", "ja")).thenReturn(details)
        `when`(detailSnapshotRepository.findById(10L)).thenReturn(Optional.empty())
        `when`(detailSnapshotRepository.save(any(PlaceDetailSnapshot::class.java))).thenAnswer { it.arguments[0] }

        val refreshed = service.refreshDetailsById(10L, "ja")

        assertEquals(true, refreshed)
        assertEquals("places/place-1/photos/photo-1", place.detailSnapshot?.primaryPhotoName)
        verify(placeSearchGateway).getPlaceDetails("place-1", "ja")
    }
}
