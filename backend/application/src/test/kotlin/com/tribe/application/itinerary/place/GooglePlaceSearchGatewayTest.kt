package com.tribe.application.itinerary.place

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

class GooglePlaceSearchGatewayTest {
    private val objectMapper = ObjectMapper()
    private val gateway = GooglePlaceSearchGateway(
        webClientBuilder = WebClient.builder(),
        objectMapper = objectMapper,
        apiKey = "test-key",
    )

    @Test
    fun `buildSearchRequestBody returns null for blank query`() {
        val body = gateway.buildSearchRequestBody(
            query = "   ",
            language = "ko",
            context = PlaceSearchContext(regionCode = "JP"),
        )

        assertNull(body)
    }

    @Test
    fun `buildSearchRequestBody normalizes region and clamps radius`() {
        val body = gateway.buildSearchRequestBody(
            query = " tower ",
            language = "ko",
            context = PlaceSearchContext(
                regionCode = "jp ",
                latitude = 35.0,
                longitude = 139.0,
                radiusMeters = 500_000,
            ),
        )

        requireNotNull(body)
        assertEquals("tower", body["textQuery"])
        assertEquals("JP", body["regionCode"])
        val locationBias = body["locationBias"] as Map<*, *>
        val circle = locationBias["circle"] as Map<*, *>
        assertEquals(50_000, circle["radius"])
    }

    @Test
    fun `buildSearchRequestBody omits invalid region code`() {
        val body = gateway.buildSearchRequestBody(
            query = "tower",
            language = "ko",
            context = PlaceSearchContext(regionCode = "JPN"),
        )

        requireNotNull(body)
        assertFalse(body.containsKey("regionCode"))
    }

    @Test
    fun `buildNearbySearchRequestBody uses included types distance rank and location restriction`() {
        val body = gateway.buildNearbySearchRequestBody(
            PlaceSearchGateway.NearbySearchRequest(
                latitude = 35.6812,
                longitude = 139.7671,
                radiusMeters = 1000,
                maxResultCount = 10,
                category = NearbyPlaceCategory.CAFE,
                language = "ko",
                region = "JP",
            ),
        )

        assertEquals(listOf("cafe", "coffee_shop"), body["includedTypes"])
        assertFalse(body.containsKey("includedPrimaryTypes"))
        assertEquals(10, body["maxResultCount"])
        assertEquals("ko", body["languageCode"])
        assertEquals("DISTANCE", body["rankPreference"])
        assertEquals("JP", body["regionCode"])
        val locationRestriction = body["locationRestriction"] as Map<*, *>
        val circle = locationRestriction["circle"] as Map<*, *>
        val center = circle["center"] as Map<*, *>
        assertEquals(35.6812, center["latitude"])
        assertEquals(139.7671, center["longitude"])
        assertEquals(1000, circle["radius"])
    }

    @Test
    fun `nearby category mapping matches approved included types`() {
        assertEquals(listOf("restaurant"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.RESTAURANT))
        assertEquals(listOf("cafe", "coffee_shop"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.CAFE))
        assertEquals(listOf("bakery"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.BAKERY))
        assertEquals(listOf("bar", "pub"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.BAR))
        assertEquals(listOf("tourist_attraction"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.ATTRACTION))
        assertEquals(
            listOf("shopping_mall", "department_store", "market", "store"),
            gateway.googleIncludedTypesFor(NearbyPlaceCategory.SHOPPING),
        )
        assertEquals(listOf("park"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.PARK))
        assertEquals(listOf("museum", "art_gallery"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.MUSEUM))
        assertEquals(listOf("hotel", "hostel", "lodging", "resort_hotel"), gateway.googleIncludedTypesFor(NearbyPlaceCategory.STAY))
    }

    @Test
    fun `nearby field mask is the approved lightweight field mask`() {
        assertEquals(
            "places.id,places.displayName,places.formattedAddress,places.location,places.primaryType,places.types",
            GooglePlaceSearchGateway.NEARBY_FIELD_MASK,
        )
        assertFalse(GooglePlaceSearchGateway.NEARBY_FIELD_MASK.contains("rating"))
        assertFalse(GooglePlaceSearchGateway.NEARBY_FIELD_MASK.contains("photos"))
        assertFalse(GooglePlaceSearchGateway.NEARBY_FIELD_MASK.contains("openingHours"))
        assertFalse(GooglePlaceSearchGateway.NEARBY_FIELD_MASK.contains("editorialSummary"))
    }

    @Test
    fun `toNearbySearchHit drops places without location`() {
        val hit = gateway.toNearbySearchHit(
            GooglePlaceSearchGateway.PlacesResponse.PlaceResult(
                id = "places/missing-location",
                formattedAddress = "Unknown",
                location = null,
                displayName = GooglePlaceSearchGateway.PlacesResponse.DisplayName(
                    text = "Missing Location Cafe",
                    languageCode = "ko",
                ),
                primaryType = "cafe",
                types = listOf("cafe"),
            ),
        )

        assertNull(hit)
    }

    @Test
    fun `toNearbySearchHit preserves valid nearby coordinates`() {
        val hit = gateway.toNearbySearchHit(
            GooglePlaceSearchGateway.PlacesResponse.PlaceResult(
                id = "places/tokyo-cafe",
                formattedAddress = "Tokyo",
                location = GooglePlaceSearchGateway.PlacesResponse.Location(
                    latitude = 35.6812,
                    longitude = 139.7671,
                ),
                displayName = GooglePlaceSearchGateway.PlacesResponse.DisplayName(
                    text = "Tokyo Cafe",
                    languageCode = "ko",
                ),
                primaryType = "cafe",
                types = listOf("cafe", "coffee_shop"),
            ),
        )

        requireNotNull(hit)
        assertEquals("places/tokyo-cafe", hit.externalPlaceId)
        assertEquals(35.6812, hit.latitude)
        assertEquals(139.7671, hit.longitude)
    }

    @Test
    fun `searchNearby maps google timeout to external api error`() {
        val timeoutGateway = GooglePlaceSearchGateway(
            webClientBuilder = WebClient.builder().exchangeFunction { Mono.never() },
            objectMapper = ObjectMapper(),
            apiKey = "test-key",
            timeoutMillis = 1,
        )

        val exception = assertThrows(BusinessException::class.java) {
            timeoutGateway.searchNearby(
                PlaceSearchGateway.NearbySearchRequest(
                    latitude = 35.6812,
                    longitude = 139.7671,
                    radiusMeters = 1000,
                    maxResultCount = 10,
                    category = NearbyPlaceCategory.CAFE,
                    language = "ko",
                    region = "JP",
                ),
            )
        }

        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
    }

    @Test
    fun `place summary field mask is the approved lightweight field mask`() {
        assertEquals(
            "id,displayName,formattedAddress,location,primaryType,types",
            GooglePlaceSearchGateway.PLACE_SUMMARY_FIELD_MASK,
        )
        assertFalse(GooglePlaceSearchGateway.PLACE_SUMMARY_FIELD_MASK.contains("rating"))
        assertFalse(GooglePlaceSearchGateway.PLACE_SUMMARY_FIELD_MASK.contains("photos"))
        assertFalse(GooglePlaceSearchGateway.PLACE_SUMMARY_FIELD_MASK.contains("openingHours"))
        assertFalse(GooglePlaceSearchGateway.PLACE_SUMMARY_FIELD_MASK.contains("editorialSummary"))
    }

    @Test
    fun `parsePriceLevel maps google enum strings to numeric levels`() {
        assertEquals(2, gateway.parsePriceLevel("PRICE_LEVEL_MODERATE"))
        assertEquals(4, gateway.parsePriceLevel("PRICE_LEVEL_VERY_EXPENSIVE"))
        assertEquals(null, gateway.parsePriceLevel("PRICE_LEVEL_UNSPECIFIED"))
        assertEquals(null, gateway.parsePriceLevel("UNKNOWN"))
    }

    @Test
    fun `details field mask includes photos`() {
        assertTrue(GooglePlaceSearchGateway.DETAILS_FIELD_MASK.split(",").contains("photos"))
    }

    @Test
    fun `search field mask requests card metadata`() {
        val fields = GooglePlaceSearchGateway.SEARCH_FIELD_MASK.split(",")

        assertTrue(fields.contains("places.rating"))
        assertTrue(fields.contains("places.userRatingCount"))
        assertTrue(fields.contains("places.businessStatus"))
        assertTrue(fields.contains("places.currentOpeningHours"))
    }

    @Test
    fun `toDetailsPayload maps first google photo name`() {
        val payload = gateway.toDetailsPayload(
            detailsResponse(
                photos = listOf(
                    GooglePlaceSearchGateway.PlaceDetailsResponse.Photo("places/place-1/photos/photo-1"),
                    GooglePlaceSearchGateway.PlaceDetailsResponse.Photo("places/place-1/photos/photo-2"),
                ),
            ),
        )

        assertEquals("places/place-1/photos/photo-1", payload.primaryPhotoName)
    }

    @Test
    fun `toDetailsPayload maps valid regular opening periods`() {
        val payload = gateway.toDetailsPayload(
            detailsResponse(
                regularOpeningHours = objectMapper.readTree(
                    """
                    {
                      "periods": [
                        {
                          "open": {"day": 0, "hour": 9, "minute": 30},
                          "close": {"day": 0, "hour": 18, "minute": 0}
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(1, payload.regularOpeningPeriods.size)
        assertEquals(0, payload.regularOpeningPeriods.first().dayOfWeek)
        assertEquals(9 * 60 + 30, payload.regularOpeningPeriods.first().openMinute)
        assertEquals(18 * 60, payload.regularOpeningPeriods.first().closeMinute)
    }

    @Test
    fun `toDetailsPayload ignores malformed regular opening period values`() {
        val invalidOpeningHours = listOf(
            """{"periods":[{"open":{"day":"0","hour":9,"minute":0},"close":{"day":0,"hour":18,"minute":0}}]}""",
            """{"periods":[{"open":{"day":7,"hour":9,"minute":0},"close":{"day":0,"hour":18,"minute":0}}]}""",
            """{"periods":[{"open":{"day":0,"hour":24,"minute":0},"close":{"day":0,"hour":18,"minute":0}}]}""",
            """{"periods":[{"open":{"day":0,"hour":9,"minute":60},"close":{"day":0,"hour":18,"minute":0}}]}""",
            """{"periods":[{"open":{"day":0,"hour":9,"minute":0},"close":null}]}""",
        )

        invalidOpeningHours.forEach { json ->
            val payload = gateway.toDetailsPayload(
                detailsResponse(regularOpeningHours = objectMapper.readTree(json)),
            )

            assertTrue(payload.regularOpeningPeriods.isEmpty(), json)
        }
    }

    @Test
    fun `getPlaceDetails retries transient server failure once before mapping external error`() {
        val attempts = AtomicInteger()
        val failingGateway = GooglePlaceSearchGateway(
            webClientBuilder = WebClient.builder().exchangeFunction {
                attempts.incrementAndGet()
                Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())
            },
            objectMapper = objectMapper,
            apiKey = "test-key",
        )

        val exception = assertThrows(BusinessException::class.java) {
            failingGateway.getPlaceDetails("place-1", "ko")
        }

        assertEquals(2, attempts.get())
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
        val detail = exception.detail as Map<*, *>
        assertEquals("google_place_details", detail["operation"])
        assertEquals("place-1", detail["externalPlaceId"])
        assertEquals(500, detail["status"])
        assertEquals("http_status", detail["cause"])
        assertEquals(true, detail["retryable"])
    }

    @Test
    fun `getPlaceDetails maps client failure without retrying`() {
        val attempts = AtomicInteger()
        val failingGateway = GooglePlaceSearchGateway(
            webClientBuilder = WebClient.builder().exchangeFunction {
                attempts.incrementAndGet()
                Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build())
            },
            objectMapper = objectMapper,
            apiKey = "test-key",
        )

        val exception = assertThrows(BusinessException::class.java) {
            failingGateway.getPlaceDetails("place-1", "ko")
        }

        assertEquals(1, attempts.get())
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
        val detail = exception.detail as Map<*, *>
        assertEquals(403, detail["status"])
        assertEquals(false, detail["retryable"])
    }

    private fun detailsResponse(
        regularOpeningHours: JsonNode? = null,
        currentOpeningHours: JsonNode? = null,
        photos: List<GooglePlaceSearchGateway.PlaceDetailsResponse.Photo>? = null,
    ): GooglePlaceSearchGateway.PlaceDetailsResponse =
        GooglePlaceSearchGateway.PlaceDetailsResponse(
            id = "place-1",
            formattedAddress = "Tokyo",
            location = GooglePlaceSearchGateway.PlacesResponse.Location(1.0, 10.0),
            displayName = GooglePlaceSearchGateway.PlacesResponse.DisplayName("Tokyo Tower", "en"),
            primaryType = "tourist_attraction",
            types = listOf("tourist_attraction"),
            businessStatus = "OPERATIONAL",
            utcOffsetMinutes = 540,
            nationalPhoneNumber = null,
            internationalPhoneNumber = null,
            websiteUri = null,
            googleMapsUri = null,
            rating = 4.7,
            userRatingCount = 100,
            priceLevel = null,
            regularOpeningHours = regularOpeningHours,
            currentOpeningHours = currentOpeningHours,
            photos = photos,
            editorialSummary = null,
        )
}
