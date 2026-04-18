package com.tribe.application.trip.review

import com.tribe.application.trip.ai.GeminiGateway
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceCatalogService
import com.tribe.application.itinerary.place.PlaceResult
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.member.Member
import com.tribe.domain.trip.review.RecommendedPlaceRepository
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripRole
import com.tribe.domain.trip.review.TripReview
import com.tribe.domain.trip.review.TripReviewRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verifyNoInteractions
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
    @Mock private lateinit var placeCatalogService: PlaceCatalogService
    @Mock private lateinit var recommendedPlaceRepository: RecommendedPlaceRepository

    private lateinit var service: TripReviewService

    @BeforeEach
    fun setUp() {
        service = TripReviewService(
            tripRepository = tripRepository,
            tripReviewRepository = tripReviewRepository,
            geminiGateway = geminiGateway,
            placeSearchService = placeSearchService,
            placeCatalogService = placeCatalogService,
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
        `when`(
            placeSearchService.search(
                "오사카 성",
                "ko",
                trip.country.code,
                34.6937,
                135.5023,
                50_000,
                "region:JP_OSAKA_KYOTO",
            ),
        ).thenReturn(
            listOf(
                PlaceResult.SearchItem(
                    externalPlaceId = "place1",
                    placeName = "오사카 성",
                    address = "오사카",
                    latitude = 1.0,
                    longitude = 10.0,
                ),
            ),
        )
        doReturn(place)
            .`when`(placeCatalogService)
            .getOrCreateAndEnrich(
                "place1",
                "오사카 성",
                "오사카",
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(10.0),
                "ko",
            )
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
    fun `createReview uses restored review prompt contract`() {
        val trip = tripFixture()
        var capturedPrompt: String? = null
        `when`(tripRepository.findTripWithFullItineraryById(trip.id)).thenReturn(trip)
        doAnswer { invocation ->
            capturedPrompt = invocation.arguments[0] as String
            "## 테스트 여행 상세 검토 및 제안\n본문"
        }
            .`when`(geminiGateway)
            .generate(org.mockito.ArgumentMatchers.anyString())
        doAnswer { invocation ->
            val saved = invocation.arguments[0] as TripReview
            ReflectionTestUtils.setField(saved, "id", 12L)
            saved
        }.`when`(tripReviewRepository).save(org.mockito.ArgumentMatchers.any(TripReview::class.java))

        service.createReview(trip.id, TripReviewCommand.Create(trip.id, "가성비 맛집 탐방"))

        val prompt = capturedPrompt ?: error("Prompt was not captured")

        assertContains(prompt, "당신은 최고의 여행 전문가이자 꼼꼼한 여행 계획 검토자입니다.")
        assertContains(prompt, "계획 검토를 중단합니다.")
        assertContains(prompt, "## 테스트 여행 수정 제안")
        assertContains(prompt, "## 테스트 여행 상세 검토 및 제안")
        assertContains(prompt, "https://www.google.com/maps/search/?api=1&query=")
        assertContains(prompt, "[사용자들의 위시리스트]")
        assertContains(prompt, "오사카 성")
        assertContains(prompt, "---추천 장소 목록---")
        assertContains(prompt, "추천 장소 목록 구간에서는 장소 이름만 한 줄에 하나씩 적고")
        assertContains(prompt, "입력에 없는 사실은 만들지 마세요.")
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
    fun `createReview keeps review when separator is missing`() {
        val trip = tripFixture()
        `when`(tripRepository.findTripWithFullItineraryById(trip.id)).thenReturn(trip)
        doReturn("## 테스트 여행 상세 검토 및 제안\n본문만 있습니다.")
            .`when`(geminiGateway)
            .generate(org.mockito.ArgumentMatchers.anyString())
        doAnswer { invocation ->
            val saved = invocation.arguments[0] as TripReview
            ReflectionTestUtils.setField(saved, "id", 13L)
            saved
        }.`when`(tripReviewRepository).save(org.mockito.ArgumentMatchers.any(TripReview::class.java))

        val result = service.createReview(trip.id, TripReviewCommand.Create(trip.id, "컨셉"))

        assertEquals(13L, result.reviewId)
        assertEquals(0, result.recommendedPlaces.size)
        verifyNoInteractions(placeSearchService, placeCatalogService, recommendedPlaceRepository)
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
        val trip = Trip("테스트 여행", LocalDate.now(), LocalDate.now().plusDays(5), Country.JAPAN, "JP_OSAKA_KYOTO")
        ReflectionTestUtils.setField(trip, "id", 5L)
        val place = Place("seed", "도톤보리", "오사카", BigDecimal.ZERO, BigDecimal.ZERO)
        trip.itineraryItems.add(ItineraryItem(trip, 1, place, null, null, 1, null))
        val wishlistPlace = Place("wish", "오사카 성", "오사카", BigDecimal.ONE, BigDecimal.ONE)
        val member = Member(email = "tester@example.com", passwordHash = "pw")
        val tripMember = trip.addMember(member, TripRole.OWNER)
        trip.wishlistItems.add(WishlistItem(trip, wishlistPlace, tripMember))
        return trip
    }

    private fun assertContains(actual: String, expected: String) {
        assertTrue(actual.contains(expected), "Expected prompt to contain <$expected> but was:\n$actual")
    }
}
