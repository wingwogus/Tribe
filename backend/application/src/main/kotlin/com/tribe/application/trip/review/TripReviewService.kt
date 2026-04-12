package com.tribe.application.trip.review

import com.tribe.application.trip.ai.GeminiGateway
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.trip.review.RecommendedPlace
import com.tribe.domain.trip.review.RecommendedPlaceRepository
import com.tribe.domain.trip.review.TripReview
import com.tribe.domain.trip.review.TripReviewRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@ConditionalOnProperty(name = ["tribe.trip.review.enabled"], havingValue = "true", matchIfMissing = true)
@Transactional
class TripReviewService(
    private val tripRepository: com.tribe.domain.trip.core.TripRepository,
    private val tripReviewRepository: TripReviewRepository,
    private val geminiGateway: GeminiGateway,
    private val placeSearchService: PlaceSearchService,
    private val placeRepository: PlaceRepository,
    private val recommendedPlaceRepository: RecommendedPlaceRepository,
) {
    @PreAuthorize("@tripAuthorizationPolicy.isTripMember(#tripId)")
    fun createReview(tripId: Long, command: TripReviewCommand.Create): TripReviewResult.ReviewDetail {
        val trip = tripRepository.findTripWithFullItineraryById(tripId)
            ?: throw BusinessException(ErrorCode.TRIP_NOT_FOUND)

        val aiFeedback = geminiGateway.generate(createPromptFromTrip(trip, command.concept))
            ?: throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)

        val (reviewContent, placePart) = splitAiResponse(aiFeedback)
        val review = tripReviewRepository.save(TripReview(trip, command.concept, reviewContent))
        parseAndRetrievePlaces(placePart, trip.country.code).forEach {
            recommendedPlaceRepository.save(RecommendedPlace.from(it, review))
        }
        return TripReviewResult.ReviewDetail.from(review)
    }

    @PreAuthorize("@tripAuthorizationPolicy.isTripMember(#tripId)")
    @Transactional(readOnly = true)
    fun getAllReviews(tripId: Long, pageable: Pageable): Page<TripReviewResult.SimpleReviewInfo> {
        return tripReviewRepository.findTripReviewsByTripId(tripId, pageable).map(TripReviewResult.SimpleReviewInfo::from)
    }

    @PreAuthorize("@tripAuthorizationPolicy.isTripMember(#tripId)")
    @Transactional(readOnly = true)
    fun getReview(tripId: Long, reviewId: Long): TripReviewResult.ReviewDetail {
        val review = tripReviewRepository.findTripReviewWithRecommendedPlacesById(reviewId)
            ?: throw BusinessException(ErrorCode.TRIP_REVIEW_NOT_FOUND)
        if (review.trip.id != tripId) throw BusinessException(ErrorCode.TRIP_NOT_FOUND)
        return TripReviewResult.ReviewDetail.from(review)
    }

    private fun parseAndRetrievePlaces(placesPart: String, countryCode: String): List<Place> {
        val placeNames = placesPart.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return placeNames.mapNotNull { placeName ->
            placeSearchService.search(placeName, "ko", countryCode).firstOrNull()?.let { searchResult ->
                placeRepository.findByExternalPlaceId(searchResult.externalPlaceId)
                    ?: placeRepository.save(
                        Place(
                            searchResult.externalPlaceId,
                            searchResult.placeName,
                            searchResult.address,
                            BigDecimal.valueOf(searchResult.latitude),
                            BigDecimal.valueOf(searchResult.longitude),
                        )
                    )
            }
        }
    }

    private fun splitAiResponse(content: String): Pair<String, String> {
        val separator = "---추천 장소 목록---"
        val parts = content.split(separator)
        return Pair(parts[0].trim(), if (parts.size > 1) parts[1] else "")
    }

    private fun createPromptFromTrip(trip: com.tribe.domain.trip.core.Trip, concept: String?): String {
        val itineraryText = trip.categories
            .groupBy { it.day }
            .toSortedMap()
            .map { (day, categories) ->
                buildString {
                    appendLine("- Day $day")
                    categories.forEach { category ->
                        appendLine("  - 카테고리: ${category.name}")
                        category.itineraryItems.forEach { item ->
                            appendLine("    - 이름: ${item.place?.name ?: item.title}, 주소: ${item.place?.address}")
                        }
                    }
                }
            }
            .joinToString("\n")

        return """
        여행 제목: ${trip.title}
        여행 국가: ${trip.country.koreanName}
        여행 기간: ${trip.startDate} ~ ${trip.endDate}
        여행 콘셉트: ${concept ?: "일반 여행"}

        상세 일정:
        $itineraryText

        아래 형식으로 여행 리뷰를 작성하고 마지막에 '---추천 장소 목록---' 아래에 추천 장소명을 줄 단위로 나열하세요.
        """.trimIndent()
    }
}
