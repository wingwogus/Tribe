package com.tribe.api.itinerary.place

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.itinerary.place.NearbyPlaceCategory
import com.tribe.application.itinerary.place.PlaceSearchContext
import com.tribe.application.itinerary.place.PlaceSearchGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class GooglePlaceSearchGatewayTest {
    private val gateway = GooglePlaceSearchGateway(
        webClientBuilder = WebClient.builder(),
        objectMapper = ObjectMapper(),
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
    fun `buildNearbySearchRequestBody uses included types and location restriction`() {
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
    fun `parsePriceLevel maps google enum strings to numeric levels`() {
        assertEquals(2, gateway.parsePriceLevel("PRICE_LEVEL_MODERATE"))
        assertEquals(4, gateway.parsePriceLevel("PRICE_LEVEL_VERY_EXPENSIVE"))
        assertEquals(null, gateway.parsePriceLevel("PRICE_LEVEL_UNSPECIFIED"))
        assertEquals(null, gateway.parsePriceLevel("UNKNOWN"))
    }
}
