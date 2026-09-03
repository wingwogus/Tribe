package com.tribe.api.itinerary.place

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.OpeningSummary
import com.tribe.application.itinerary.place.OpeningSummarySource
import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceResult
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.security.TokenProvider
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PlaceController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class PlaceControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var placeSearchService: PlaceSearchService
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `resolveExternalPlace returns resolved saved place response`() {
        `when`(
            placeSearchService.resolveExternalPlace(
                externalPlaceId = "google-place-1",
                language = "ko",
            ),
        ).thenReturn(
            PlaceResult.SearchItem(
                placeId = 10L,
                externalPlaceId = "google-place-1",
                placeName = "Tokyo Tower",
                address = "Tokyo",
                latitude = 35.6586,
                longitude = 139.7454,
            ),
        )

        mockMvc.perform(
            post("/api/v1/places/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"externalPlaceId":"google-place-1","language":"ko"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.placeId", equalTo(10)))
            .andExpect(jsonPath("$.data.externalPlaceId", equalTo("google-place-1")))
            .andExpect(jsonPath("$.data.placeName", equalTo("Tokyo Tower")))
    }

    @Test
    fun `searchNearbyPlaces returns nearby search response`() {
        `when`(
            placeSearchService.searchNearby(
                latitude = 35.6812,
                longitude = 139.7671,
                radiusMeters = 1000,
                maxResultCount = 10,
                category = "CAFE",
                language = "ko",
                region = "JP",
            ),
        ).thenReturn(
            listOf(
                PlaceResult.SearchItem(
                    placeId = null,
                    externalPlaceId = "google-place-1",
                    placeName = "Tokyo Cafe",
                    address = "Tokyo",
                    latitude = 35.6812,
                    longitude = 139.7671,
                    photoHint = PlaceResult.PhotoHint(name = "places/google-place-1/photos/1"),
                    placeDetailSummary = PlaceDetailSummary(
                        businessStatus = "OPERATIONAL",
                        rating = 4.7,
                        userRatingCount = 123,
                        editorialSummary = "Known local cafe.",
                    ),
                    openingSummary = OpeningSummary(
                        openNow = true,
                        nextOpenTime = null,
                        nextCloseTime = "2026-05-17T14:00:00+09:00",
                        source = OpeningSummarySource.CURRENT,
                        timezoneOffsetMinutes = 540,
                        syncedAt = null,
                        stale = false,
                    ),
                ),
            ),
        )

        mockMvc.perform(
            post("/api/v1/places/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "latitude": 35.6812,
                      "longitude": 139.7671,
                      "radiusMeters": 1000,
                      "maxResultCount": 10,
                      "category": "CAFE",
                      "language": "ko",
                      "region": "JP"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].externalPlaceId", equalTo("google-place-1")))
            .andExpect(jsonPath("$.data[0].placeName", equalTo("Tokyo Cafe")))
            .andExpect(jsonPath("$.data[0].photoHint").doesNotExist())
            .andExpect(jsonPath("$.data[0].placeDetailSummary").doesNotExist())
            .andExpect(jsonPath("$.data[0].openingSummary").doesNotExist())
    }

    @Test
    fun `getPlaceDetail returns price level in detail response`() {
        `when`(
            placeSearchService.getPlaceDetail(10L, "ko"),
        ).thenReturn(
            PlaceResult.Detail(
                placeId = 10L,
                externalPlaceId = "google-place-1",
                placeName = "Tokyo Cafe",
                address = "Tokyo",
                latitude = 35.6812,
                longitude = 139.7671,
                placeTypeSummary = null,
                normalizedCategoryKey = null,
                photoHint = null,
                placeDetailSummary = null,
                formattedPhoneNumber = null,
                internationalPhoneNumber = null,
                websiteUri = null,
                googleMapsUri = null,
                priceLevel = 2,
                regularOpeningHoursJson = null,
                currentOpeningHoursJson = null,
            ),
        )

        mockMvc.perform(get("/api/v1/places/10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.placeId", equalTo(10)))
            .andExpect(jsonPath("$.data.priceLevel", equalTo(2)))
    }

    @Test
    fun `searchNearbyPlaces returns bad request for invalid latitude`() {
        `when`(
            placeSearchService.searchNearby(91.0, 139.7671, 1000, 10, "CAFE", "ko", "JP"),
        ).thenThrow(invalidInput("latitude", 91.0))

        assertNearbyBadRequest(
            """
            {
              "latitude": 91.0,
              "longitude": 139.7671,
              "radiusMeters": 1000,
              "maxResultCount": 10,
              "category": "CAFE",
              "language": "ko",
              "region": "JP"
            }
            """.trimIndent(),
            "latitude",
        )
    }

    @Test
    fun `searchNearbyPlaces returns bad request for invalid radius`() {
        `when`(
            placeSearchService.searchNearby(35.6812, 139.7671, 0, 10, "CAFE", "ko", "JP"),
        ).thenThrow(invalidInput("radiusMeters", 0))

        assertNearbyBadRequest(
            """
            {
              "latitude": 35.6812,
              "longitude": 139.7671,
              "radiusMeters": 0,
              "maxResultCount": 10,
              "category": "CAFE",
              "language": "ko",
              "region": "JP"
            }
            """.trimIndent(),
            "radiusMeters",
        )
    }

    @Test
    fun `searchNearbyPlaces returns bad request for invalid max result count`() {
        `when`(
            placeSearchService.searchNearby(35.6812, 139.7671, 1000, 21, "CAFE", "ko", "JP"),
        ).thenThrow(invalidInput("maxResultCount", 21))

        assertNearbyBadRequest(
            """
            {
              "latitude": 35.6812,
              "longitude": 139.7671,
              "radiusMeters": 1000,
              "maxResultCount": 21,
              "category": "CAFE",
              "language": "ko",
              "region": "JP"
            }
            """.trimIndent(),
            "maxResultCount",
        )
    }

    @Test
    fun `searchNearbyPlaces returns bad request for unsupported category`() {
        `when`(
            placeSearchService.searchNearby(35.6812, 139.7671, 1000, 10, "DINER", "ko", "JP"),
        ).thenThrow(invalidInput("category", "DINER"))

        assertNearbyBadRequest(
            """
            {
              "latitude": 35.6812,
              "longitude": 139.7671,
              "radiusMeters": 1000,
              "maxResultCount": 10,
              "category": "DINER",
              "language": "ko",
              "region": "JP"
            }
            """.trimIndent(),
            "category",
        )
    }

    private fun assertNearbyBadRequest(payload: String, expectedField: String) {
        mockMvc.perform(
            post("/api/v1/places/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo(ErrorCode.INVALID_INPUT.code)))
            .andExpect(jsonPath("$.error.detail.field", equalTo(expectedField)))
    }

    private fun invalidInput(field: String, rejectedValue: Any?) =
        BusinessException(
            ErrorCode.INVALID_INPUT,
            detail = mapOf(
                "field" to field,
                "rejectedValue" to rejectedValue,
            ),
        )
}
