package com.tribe.application.itinerary.place

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshotRepository
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriodRepository
import com.tribe.domain.itinerary.place.PlaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
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
        `when`(transactionManager.getTransaction(any(TransactionDefinition::class.java)))
            .thenReturn(SimpleTransactionStatus())
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
        val concurrentPlace = Place("place-1", "Tokyo Tower", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(concurrentPlace, "id", 10L)
        concurrentPlace.detailsSyncedAt = LocalDateTime.now()

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
}
