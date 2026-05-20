package com.tribe.application.itinerary.place

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

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
