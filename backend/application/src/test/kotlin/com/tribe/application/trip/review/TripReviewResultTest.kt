package com.tribe.application.trip.review

import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.review.RecommendedPlace
import com.tribe.domain.trip.review.TripReview
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class TripReviewResultTest {
    @Test
    fun `review detail maps recommended places with saved place fragments`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val review = TripReview(trip, "맛집", "## 제목\n본문")
        ReflectionTestUtils.setField(review, "id", 70L)
        review.createdAt = LocalDateTime.of(2026, 4, 16, 12, 0)
        val place = placeFixture()
        val recommendedPlace = RecommendedPlace.from(place, review)
        ReflectionTestUtils.setField(recommendedPlace, "id", 80L)

        val result = TripReviewResult.ReviewDetail.from(review)
        val recommended = result.recommendedPlaces.single()

        assertEquals(70L, result.reviewId)
        assertEquals("bakery", recommended.placeTypeSummary?.primaryType)
        assertEquals(NormalizedPlaceCategoryKey.BAKERY, recommended.normalizedCategoryKey)
        assertEquals("CLOSED_TEMPORARILY", recommended.placeDetailSummary?.businessStatus)
        assertEquals(3.9, recommended.placeDetailSummary?.rating)
        assertEquals(12, recommended.placeDetailSummary?.userRatingCount)
        assertEquals("빵이 유명함", recommended.placeDetailSummary?.editorialSummary)
    }

    private fun placeFixture(): Place {
        val place = Place(
            externalPlaceId = "place-3",
            name = "Bakery",
            address = "Kyoto",
            latitude = BigDecimal("35.0116"),
            longitude = BigDecimal("135.7681"),
        )
        ReflectionTestUtils.setField(place, "id", 33L)
        place.googlePrimaryType = "bakery"
        place.googleTypesJson = "[\"bakery\",\"food\"]"
        place.businessStatus = "CLOSED_TEMPORARILY"
        place.detailSnapshot = PlaceDetailSnapshot(
            place = place,
            rating = 3.9,
            userRatingCount = 12,
            editorialSummary = "빵이 유명함",
        )
        return place
    }
}
