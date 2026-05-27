package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal

class PlaceResultAssemblerTest {
    private val assembler = PlaceResultAssembler()

    @Test
    fun `toDetail keeps derived place fragments aligned`() {
        val place = placeFixture()

        val detailView = assembler.toDetail(place)

        assertEquals("japanese_restaurant", detailView.placeTypeSummary?.primaryType)
        assertEquals(listOf("japanese_restaurant", "restaurant"), detailView.placeTypeSummary?.types)
        assertEquals("japanese restaurant", detailView.placeTypeSummary?.displayPrimaryLabel)
        assertEquals(NormalizedPlaceCategoryKey.JAPANESE_FOOD, detailView.normalizedCategoryKey)
        assertEquals("OPERATIONAL", detailView.placeDetailSummary?.businessStatus)
        assertEquals(4.7, detailView.placeDetailSummary?.rating)
        assertEquals(128, detailView.placeDetailSummary?.userRatingCount)
        assertEquals("유명한 스시집", detailView.placeDetailSummary?.editorialSummary)
    }

    @Test
    fun `derived fragments are null when place has no synced metadata`() {
        val place = Place(
            externalPlaceId = "place-2",
            name = "Empty",
            address = null,
            latitude = BigDecimal.ZERO,
            longitude = BigDecimal.ZERO,
        )

        assertNull(assembler.toPlaceTypeSummary(place))
        assertNull(assembler.toNormalizedCategoryKey(place))
        assertNull(assembler.toDetailSummary(place))
    }

    @Test
    fun `toSearchItem uses google search metadata when canonical place is missing`() {
        val openingSummary = OpeningSummary(
            openNow = true,
            nextOpenTime = null,
            nextCloseTime = "2026-05-17T14:00:00+09:00",
            source = OpeningSummarySource.CURRENT,
            timezoneOffsetMinutes = 540,
            syncedAt = null,
            stale = false,
        )

        val item = assembler.toSearchItem(
            hit = PlaceSearchGateway.SearchHit(
                externalPlaceId = "google-place-1",
                placeName = "Kiji",
                address = "Osaka, Kita Ward, Umeda",
                latitude = 34.7,
                longitude = 135.49,
                primaryType = "japanese_restaurant",
                types = listOf("japanese_restaurant", "restaurant"),
                businessStatus = "OPERATIONAL",
                rating = 4.5,
                userRatingCount = 235,
                openingSummary = openingSummary,
            ),
            canonicalPlace = null,
        )

        assertNull(item.placeId)
        assertEquals(NormalizedPlaceCategoryKey.JAPANESE_FOOD, item.normalizedCategoryKey)
        assertEquals("OPERATIONAL", item.placeDetailSummary?.businessStatus)
        assertEquals(4.5, item.placeDetailSummary?.rating)
        assertEquals(235, item.placeDetailSummary?.userRatingCount)
        assertEquals(openingSummary, item.openingSummary)
    }

    private fun placeFixture(): Place {
        val place = Place(
            externalPlaceId = "place-1",
            name = "Sushi",
            address = "Tokyo",
            latitude = BigDecimal("35.6895"),
            longitude = BigDecimal("139.6917"),
        )
        ReflectionTestUtils.setField(place, "id", 10L)
        place.googlePrimaryType = "japanese_restaurant"
        place.googleTypesJson = "[\"japanese_restaurant\",\"restaurant\"]"
        place.businessStatus = "OPERATIONAL"
        place.detailSnapshot = PlaceDetailSnapshot(
            place = place,
            rating = 4.7,
            userRatingCount = 128,
            editorialSummary = "유명한 스시집",
        )
        return place
    }
}
