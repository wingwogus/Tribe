package com.tribe.application.trip.review

import com.tribe.application.trip.ai.GeminiGateway
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchResult
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.trip.review.RecommendedPlaceRepository
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.review.TripReview
import com.tribe.domain.trip.review.TripReviewRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class TripReviewServiceTest {
    @Mock private lateinit var tripRepository: com.tribe.domain.trip.core.TripRepository
    @Mock private lateinit var tripReviewRepository: TripReviewRepository
    @Mock private lateinit var geminiGateway: GeminiGateway
    @Mock private lateinit var placeSearchService: PlaceSearchService
    @Mock private lateinit var placeRepository: PlaceRepository
    @Mock private lateinit var recommendedPlaceRepository: RecommendedPlaceRepository

    private lateinit var service: TripReviewService

    @BeforeEach
    fun setUp() {
        service = TripReviewService(
            tripRepository = tripRepository,
            tripReviewRepository = tripReviewRepository,
            geminiGateway = geminiGateway,
            placeSearchService = placeSearchService,
            placeRepository = placeRepository,
            recommendedPlaceRepository = recommendedPlaceRepository,
        )
    }

    @Test
    fun `createReview saves review and recommended places`() {
        val trip = tripFixture()
        val place = Place("place1", "오사카 성", "오사카", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 31L)
        `when`(tripRepository.findTripWithFullItineraryById(trip.id)).thenReturn(trip)
        doReturn("## 피드백\n---추천 장소 목록---\n오사카 성")
            .`when`(geminiGateway)
            .generate(org.mockito.ArgumentMatchers.anyString())
        `when`(placeSearchService.search("오사카 성", "ko", trip.country.code)).thenReturn(
            listOf(PlaceSearchResult("place1", "오사카 성", "오사카", 1.0, 10.0)),
        )
        `when`(placeRepository.findByExternalPlaceId("place1")).thenReturn(place)
        doAnswer { invocation ->
            val saved = invocation.arguments[0] as TripReview
            ReflectionTestUtils.setField(saved, "id", 11L)
            saved
        }.`when`(tripReviewRepository).save(org.mockito.ArgumentMatchers.any(TripReview::class.java))

        val result = service.createReview(trip.id, TripReviewCommand.Create(trip.id, "럭셔리"))

        assertEquals(11L, result.reviewId)
        assertEquals(1, result.recommendedPlaces.size)
        assertEquals("오사카 성", result.recommendedPlaces.first().placeName)
    }

    @Test
    fun `createReview fails when AI feedback is missing`() {
        val trip = tripFixture()
        `when`(tripRepository.findTripWithFullItineraryById(trip.id)).thenReturn(trip)
        doReturn(null)
            .`when`(geminiGateway)
            .generate(org.mockito.ArgumentMatchers.anyString())

        val ex = assertThrows(BusinessException::class.java) {
            service.createReview(trip.id, TripReviewCommand.Create(trip.id, "컨셉"))
        }

        assertEquals(ErrorCode.AI_FEEDBACK_ERROR, ex.errorCode)
    }

    @Test
    fun `getAllReviews returns source page`() {
        val trip = tripFixture()
        val review = TripReview(trip, "컨셉", "## 제목\n본문")
        ReflectionTestUtils.setField(review, "id", 21L)
        `when`(tripReviewRepository.findTripReviewsByTripId(trip.id, PageRequest.of(0, 10)))
            .thenReturn(PageImpl(listOf(review), PageRequest.of(0, 10), 1))

        val result = service.getAllReviews(trip.id, PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals("제목", result.content.first().title)
    }

    private fun tripFixture(): Trip {
        val trip = Trip("테스트 여행", LocalDate.now(), LocalDate.now().plusDays(5), Country.JAPAN)
        ReflectionTestUtils.setField(trip, "id", 5L)
        val category = Category(trip, 1, "Day 1", 1)
        val place = Place("seed", "도톤보리", "오사카", BigDecimal.ZERO, BigDecimal.ZERO)
        category.itineraryItems.add(ItineraryItem(category, place, null, null, 1, null))
        trip.categories.add(category)
        return trip
    }
}
