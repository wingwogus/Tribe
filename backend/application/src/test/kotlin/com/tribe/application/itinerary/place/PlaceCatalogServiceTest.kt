package com.tribe.application.itinerary.place

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.place.PlaceDetailSnapshotRepository
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriodRepository
import com.tribe.domain.itinerary.place.PlaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
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
    fun `getOrCreateAndEnrich reattaches newly created place before enrichment`() {
        `when`(transactionManager.getTransaction(any(TransactionDefinition::class.java)))
            .thenReturn(SimpleTransactionStatus())
        val createdPlace = Place("place-1", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(createdPlace, "id", 10L)
        val managedPlace = Place("place-1", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(managedPlace, "id", 10L)
        val syncedAt = LocalDateTime.now()
        managedPlace.detailsSyncedAt = syncedAt
        managedPlace.detailSnapshot = PlaceDetailSnapshot(
            place = managedPlace,
            openingHoursSyncedAt = syncedAt,
            currentOpeningHoursSyncedAt = syncedAt,
        )

        `when`(placeRepository.findByExternalPlaceId("place-1")).thenReturn(null)
        `when`(placeRepository.saveAndFlush(any(Place::class.java))).thenReturn(createdPlace)
        `when`(placeRepository.getReferenceById(10L)).thenReturn(managedPlace)

        val result = service.getOrCreateAndEnrich(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )

        assertSame(managedPlace, result)
        verifyNoInteractions(placeSearchGateway)
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
        `when`(placeRepository.getReferenceById(10L)).thenReturn(concurrentPlace)

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
    fun `getOrCreateAndEnrich returns minimal place when detail enrichment fails externally`() {
        val place = Place("place-fail", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 10L)

        `when`(placeRepository.findByExternalPlaceId("place-fail")).thenReturn(place)
        `when`(placeSearchGateway.getPlaceDetails("place-fail", "ko"))
            .thenThrow(BusinessException(ErrorCode.EXTERNAL_API_ERROR))

        val result = service.getOrCreateAndEnrich(
            externalPlaceId = "place-fail",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )

        assertSame(place, result)
        verifyNoInteractions(detailSnapshotRepository, openingPeriodRepository)
    }

    @Test
    fun `enrichDetailsIfNeeded propagates external detail failure for strict callers`() {
        val place = Place("place-strict", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 10L)

        `when`(placeSearchGateway.getPlaceDetails("place-strict", "ko"))
            .thenThrow(BusinessException(ErrorCode.EXTERNAL_API_ERROR))

        val exception = assertThrows(BusinessException::class.java) {
            service.enrichDetailsIfNeeded(place, "ko")
        }

        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
        verifyNoInteractions(detailSnapshotRepository, openingPeriodRepository)
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

    @Test
    fun `getOrCreateFromExternalPlaceId creates place from lightweight summary without details enrichment`() {
        val summary = PlaceSearchGateway.SearchHit(
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = 35.6586,
            longitude = 139.7454,
            primaryType = "tourist_attraction",
            types = listOf("tourist_attraction", "point_of_interest"),
        )
        `when`(placeRepository.findByExternalPlaceId("place-1")).thenReturn(null)
        `when`(placeSearchGateway.getPlaceSummary("place-1", "ko")).thenReturn(summary)
        `when`(placeRepository.saveAndFlush(any(Place::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Place
            ReflectionTestUtils.setField(saved, "id", 10L)
            saved
        }

        val result = service.getOrCreateFromExternalPlaceId("place-1", "ko")

        assertEquals(10L, result.id)
        assertEquals("Tokyo Tower", result.name)
        assertEquals("tourist_attraction", result.googlePrimaryType)
        verify(placeSearchGateway, never()).getPlaceDetails("place-1", "ko")
    }
}
