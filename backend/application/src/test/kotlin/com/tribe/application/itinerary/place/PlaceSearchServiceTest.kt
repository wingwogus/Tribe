package com.tribe.application.itinerary.place

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PlaceSearchServiceTest {
    @Mock private lateinit var placeSearchGateway: PlaceSearchGateway
    @Mock private lateinit var cacheRepository: PlaceSearchCacheRepository
    @Mock private lateinit var placeCatalogService: PlaceCatalogService
    @Mock private lateinit var placeRepository: PlaceRepository
    @Mock private lateinit var placeResultAssembler: PlaceResultAssembler

    private lateinit var service: PlaceSearchService

    @BeforeEach
    fun setUp() {
        service = PlaceSearchService(
            placeSearchGateway = placeSearchGateway,
            placeSearchCacheRepository = cacheRepository,
            placeCatalogService = placeCatalogService,
            placeRepository = placeRepository,
            placeResultAssembler = placeResultAssembler,
        )
    }

    @Test
    fun `search returns cached results without gateway call`() {
        val cached = listOf(
            PlaceSearchGateway.SearchHit(
                externalPlaceId = "place-1",
                placeName = "Tokyo Tower",
                address = "Tokyo",
                latitude = 1.0,
                longitude = 2.0,
            ),
        )
        val canonical = listOf(
            PlaceResult.SearchItem(
                placeId = 10L,
                externalPlaceId = "place-1",
                placeName = "Tokyo Tower",
                address = "Tokyo",
                latitude = 1.0,
                longitude = 2.0,
            ),
        )
        `when`(cacheRepository.get("tower|ko|country:JP|35.0|139.0|50000")).thenReturn(cached)
        `when`(placeCatalogService.mergeWithCanonical(cached)).thenReturn(canonical)

        val result = service.search("tower", "ko", "JP", 35.0, 139.0, 500000, "country:JP")

        assertEquals(1, result.size)
        verifyNoInteractions(placeSearchGateway)
    }

    @Test
    fun `search clamps radius before gateway call`() {
        val expectedContext = PlaceSearchContext(
            regionCode = "JP",
            latitude = 35.0,
            longitude = 139.0,
            radiusMeters = 50_000,
            regionContextKey = "country:JP",
        )
        `when`(cacheRepository.get("tower|ko|country:JP|35.0|139.0|50000")).thenReturn(null)
        `when`(placeSearchGateway.search("tower", "ko", expectedContext)).thenReturn(emptyList())
        `when`(placeCatalogService.mergeWithCanonical(emptyList())).thenReturn(emptyList())

        val result = service.search("tower", "ko", "JP", 35.0, 139.0, 500000, "country:JP")

        assertEquals(0, result.size)
        verify(placeSearchGateway).search("tower", "ko", expectedContext)
    }

    @Test
    fun `search defaults radius and normalizes region when coordinates exist`() {
        val expectedContext = PlaceSearchContext(
            regionCode = "JP",
            latitude = 35.0,
            longitude = 139.0,
            radiusMeters = 50_000,
            regionContextKey = "country:JP",
        )
        `when`(cacheRepository.get("tower|ko|country:JP|35.0|139.0|50000")).thenReturn(null)
        `when`(placeSearchGateway.search("tower", "ko", expectedContext)).thenReturn(emptyList())
        `when`(placeCatalogService.mergeWithCanonical(emptyList())).thenReturn(emptyList())

        service.search("tower", "ko", "jp", 35.0, 139.0, null, "country:JP")

        verify(placeSearchGateway).search("tower", "ko", expectedContext)
    }

    @Test
    fun `searchNearby normalizes input and caches using quantized nearby key`() {
        val gatewayHits = listOf(
            PlaceSearchGateway.SearchHit(
                externalPlaceId = "nearby-1",
                placeName = "Cafe",
                address = "Tokyo",
                latitude = 35.6812,
                longitude = 139.7671,
            ),
        )
        val canonical = listOf(
            PlaceResult.SearchItem(
                externalPlaceId = "nearby-1",
                placeName = "Cafe",
                address = "Tokyo",
                latitude = 35.6812,
                longitude = 139.7671,
            ),
        )
        val expectedRequest = PlaceSearchGateway.NearbySearchRequest(
            latitude = 35.681234,
            longitude = 139.767149,
            radiusMeters = 1000,
            maxResultCount = 10,
            category = NearbyPlaceCategory.CAFE,
            language = "ko",
            region = "JP",
        )
        val expectedKey = "nearby:v1|CAFE|ko|JP|1000|10|35.6812|139.7671"
        `when`(cacheRepository.get(expectedKey)).thenReturn(null)
        `when`(placeSearchGateway.searchNearby(expectedRequest)).thenReturn(gatewayHits)
        `when`(placeCatalogService.mergeWithCanonical(gatewayHits)).thenReturn(canonical)

        val result = service.searchNearby(
            latitude = 35.681234,
            longitude = 139.767149,
            radiusMeters = 1000,
            maxResultCount = 10,
            category = " cafe ",
            language = "KO ",
            region = "jp",
        )

        assertEquals(canonical, result)
        verify(placeSearchGateway).searchNearby(expectedRequest)
        verify(cacheRepository).put(expectedKey, gatewayHits, java.time.Duration.ofHours(6))
    }

    @Test
    fun `searchNearby equivalent coordinates share quantized cache key`() {
        val cached = listOf(
            PlaceSearchGateway.SearchHit(
                externalPlaceId = "nearby-1",
                placeName = "Cafe",
                address = "Tokyo",
                latitude = 35.6812,
                longitude = 139.7671,
            ),
        )
        val canonical = listOf(
            PlaceResult.SearchItem(
                externalPlaceId = "nearby-1",
                placeName = "Cafe",
                address = "Tokyo",
                latitude = 35.6812,
                longitude = 139.7671,
            ),
        )
        val expectedKey = "nearby:v1|CAFE|ko|JP|1000|10|35.6812|139.7671"
        `when`(cacheRepository.get(expectedKey)).thenReturn(cached)
        `when`(placeCatalogService.mergeWithCanonical(cached)).thenReturn(canonical)

        val result = service.searchNearby(
            latitude = 35.681249,
            longitude = 139.767149,
            radiusMeters = 1000,
            maxResultCount = 10,
            category = "CAFE",
            language = "ko",
            region = "JP",
        )

        assertEquals(canonical, result)
        verify(cacheRepository).get(expectedKey)
        verifyNoInteractions(placeSearchGateway)
    }

    @Test
    fun `searchNearby suppresses detail-only fields from canonical matches`() {
        val gatewayHits = listOf(
            PlaceSearchGateway.SearchHit(
                externalPlaceId = "nearby-1",
                placeName = "Cafe",
                address = "Tokyo",
                latitude = 35.6812,
                longitude = 139.7671,
            ),
        )
        val canonical = listOf(
            PlaceResult.SearchItem(
                placeId = 10L,
                externalPlaceId = "nearby-1",
                placeName = "Cafe",
                address = "Tokyo",
                latitude = 35.6812,
                longitude = 139.7671,
                photoHint = PlaceResult.PhotoHint(name = "places/nearby-1/photos/1"),
                placeDetailSummary = PlaceDetailSummary(
                    businessStatus = "OPERATIONAL",
                    rating = 4.7,
                    userRatingCount = 123,
                    editorialSummary = "Known local cafe.",
                ),
            ),
        )
        val expectedRequest = PlaceSearchGateway.NearbySearchRequest(
            latitude = 35.6812,
            longitude = 139.7671,
            radiusMeters = 1000,
            maxResultCount = 10,
            category = NearbyPlaceCategory.CAFE,
            language = "ko",
            region = "JP",
        )
        `when`(cacheRepository.get("nearby:v1|CAFE|ko|JP|1000|10|35.6812|139.7671")).thenReturn(null)
        `when`(placeSearchGateway.searchNearby(expectedRequest)).thenReturn(gatewayHits)
        `when`(placeCatalogService.mergeWithCanonical(gatewayHits)).thenReturn(canonical)

        val result = service.searchNearby(35.6812, 139.7671, 1000, 10, "CAFE", "ko", "JP")

        assertEquals(10L, result.single().placeId)
        assertNull(result.single().photoHint)
        assertNull(result.single().placeDetailSummary)
    }

    @Test
    fun `searchNearby cache key changes for each nearby search dimension`() {
        val baseRequest = PlaceSearchGateway.NearbySearchRequest(
            latitude = 35.681234,
            longitude = 139.767149,
            radiusMeters = 1000,
            maxResultCount = 10,
            category = NearbyPlaceCategory.CAFE,
            language = "ko",
            region = "JP",
        )
        val requestKeys = listOf(
            baseRequest to "nearby:v1|CAFE|ko|JP|1000|10|35.6812|139.7671",
            baseRequest.copy(category = NearbyPlaceCategory.RESTAURANT) to
                "nearby:v1|RESTAURANT|ko|JP|1000|10|35.6812|139.7671",
            baseRequest.copy(language = "ja") to "nearby:v1|CAFE|ja|JP|1000|10|35.6812|139.7671",
            baseRequest.copy(region = "US") to "nearby:v1|CAFE|ko|US|1000|10|35.6812|139.7671",
            baseRequest.copy(radiusMeters = 2000) to "nearby:v1|CAFE|ko|JP|2000|10|35.6812|139.7671",
            baseRequest.copy(maxResultCount = 20) to "nearby:v1|CAFE|ko|JP|1000|20|35.6812|139.7671",
            baseRequest.copy(latitude = 35.681251, longitude = 139.767151) to
                "nearby:v1|CAFE|ko|JP|1000|10|35.6813|139.7672",
        )
        requestKeys.forEach { (request, _) ->
            `when`(placeSearchGateway.searchNearby(request)).thenReturn(emptyList())
        }
        requestKeys.forEach { (_, key) ->
            `when`(cacheRepository.get(key)).thenReturn(null)
        }
        `when`(placeCatalogService.mergeWithCanonical(emptyList())).thenReturn(emptyList())

        requestKeys.forEach { (request, _) ->
            service.searchNearby(
                latitude = request.latitude,
                longitude = request.longitude,
                radiusMeters = request.radiusMeters,
                maxResultCount = request.maxResultCount,
                category = request.category.name,
                language = request.language,
                region = request.region,
            )
        }

        requestKeys.forEach { (_, expectedKey) ->
            verify(cacheRepository).get(expectedKey)
            verify(cacheRepository).put(expectedKey, emptyList(), java.time.Duration.ofHours(6))
        }
    }

    @Test
    fun `searchNearby rejects invalid latitude`() {
        val exception = assertThrows(BusinessException::class.java) {
            service.searchNearby(
                latitude = 91.0,
                longitude = 139.0,
                radiusMeters = 1000,
                maxResultCount = 10,
                category = "CAFE",
                language = "ko",
                region = "JP",
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
        verifyNoInteractions(placeSearchGateway)
    }

    @Test
    fun `searchNearby rejects invalid longitude`() {
        val exception = assertThrows(BusinessException::class.java) {
            service.searchNearby(
                latitude = 35.0,
                longitude = -181.0,
                radiusMeters = 1000,
                maxResultCount = 10,
                category = "CAFE",
                language = "ko",
                region = "JP",
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
        verifyNoInteractions(placeSearchGateway)
    }

    @Test
    fun `searchNearby rejects invalid radius max result count and category`() {
        assertEquals(
            ErrorCode.INVALID_INPUT,
            assertThrows(BusinessException::class.java) {
                service.searchNearby(35.0, 139.0, 0, 10, "CAFE", "ko", "JP")
            }.errorCode,
        )
        assertEquals(
            ErrorCode.INVALID_INPUT,
            assertThrows(BusinessException::class.java) {
                service.searchNearby(35.0, 139.0, 5_001, 10, "CAFE", "ko", "JP")
            }.errorCode,
        )
        assertEquals(
            ErrorCode.INVALID_INPUT,
            assertThrows(BusinessException::class.java) {
                service.searchNearby(35.0, 139.0, 1000, 0, "CAFE", "ko", "JP")
            }.errorCode,
        )
        assertEquals(
            ErrorCode.INVALID_INPUT,
            assertThrows(BusinessException::class.java) {
                service.searchNearby(35.0, 139.0, 1000, 21, "CAFE", "ko", "JP")
            }.errorCode,
        )
        assertEquals(
            ErrorCode.INVALID_INPUT,
            assertThrows(BusinessException::class.java) {
                service.searchNearby(35.0, 139.0, 1000, 10, "DINER", "ko", "JP")
            }.errorCode,
        )
        verifyNoInteractions(placeSearchGateway)
    }

    @Test
    fun `getPlaceDetail enriches and assembles view`() {
        val place = Place(
            externalPlaceId = "place-1",
            name = "Tokyo Tower",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )
        ReflectionTestUtils.setField(place, "id", 10L)
        val view = PlaceResult.Detail(
            placeId = 10L,
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = 1.0,
            longitude = 10.0,
            placeTypeSummary = null,
            normalizedCategoryKey = null,
            photoHint = null,
            placeDetailSummary = null,
            formattedPhoneNumber = null,
            internationalPhoneNumber = null,
            websiteUri = null,
            googleMapsUri = null,
            regularOpeningHoursJson = null,
            currentOpeningHoursJson = null,
        )
        `when`(placeRepository.findById(10L)).thenReturn(Optional.of(place))
        `when`(placeCatalogService.enrichDetailsIfNeeded(place, "ko")).thenReturn(place)
        `when`(placeResultAssembler.toDetail(place)).thenReturn(view)

        val result = service.getPlaceDetail(10L, "ko")

        assertEquals(10L, result.placeId)
    }
}
