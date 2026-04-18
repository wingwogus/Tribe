package com.tribe.application.trip.review

import com.tribe.application.trip.ai.GeminiGateway
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.domain.trip.review.RecommendedPlace
import com.tribe.domain.trip.review.RecommendedPlaceRepository
import com.tribe.domain.trip.review.TripReview
import com.tribe.domain.trip.review.TripReviewRepository
import com.tribe.domain.trip.core.TripRegion
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
@ConditionalOnProperty(name = ["tribe.trip.review.enabled"], havingValue = "true", matchIfMissing = true)
@Transactional
class TripReviewService(
    private val tripRepository: com.tribe.domain.trip.core.TripRepository,
    private val tripReviewRepository: TripReviewRepository,
    private val geminiGateway: GeminiGateway,
    private val placeSearchService: PlaceSearchService,
    private val placeCatalogService: com.tribe.application.itinerary.place.PlaceCatalogService,
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
        parseAndRetrievePlaces(placePart, trip.country.code, TripRegion.from(trip.regionCode)).forEach {
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

    private fun parseAndRetrievePlaces(
        placesPart: String,
        countryCode: String,
        region: TripRegion?,
    ): List<com.tribe.domain.itinerary.place.Place> {
        val placeNames = placesPart.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return placeNames.mapNotNull { placeName ->
            placeSearchService.search(
                query = buildRegionAwareQuery(placeName, region),
                language = "ko",
                region = countryCode,
                latitude = region?.centerLat,
                longitude = region?.centerLng,
                radiusMeters = if (region != null) 50_000 else null,
                regionContextKey = region?.code?.let { "region:$it" },
            ).firstOrNull()?.let { searchResult ->
                placeCatalogService.getOrCreateAndEnrich(
                    externalPlaceId = searchResult.externalPlaceId,
                    placeName = searchResult.placeName,
                    address = searchResult.address,
                    latitude = BigDecimal.valueOf(searchResult.latitude),
                    longitude = BigDecimal.valueOf(searchResult.longitude),
                )
            }
        }
    }

    private fun buildRegionAwareQuery(placeName: String, region: TripRegion?): String {
        if (region == null) {
            return placeName
        }
        val loweredPlaceName = placeName.lowercase()
        val hasHint = listOf(region.label, *region.searchHints.toTypedArray()).any { hint ->
            loweredPlaceName.contains(hint.lowercase())
        }
        return if (hasHint) placeName else "${region.label} $placeName"
    }

    private fun splitAiResponse(content: String): Pair<String, String> {
        val separator = "---추천 장소 목록---"
        val parts = content.split(separator)
        return Pair(parts[0].trim(), if (parts.size > 1) parts[1] else "")
    }

    private fun createPromptFromTrip(trip: com.tribe.domain.trip.core.Trip, concept: String?): String {
        val itineraryText = trip.itineraryItems
            .groupBy { it.visitDay }
            .toSortedMap()
            .map { (day, items) ->
                buildString {
                    appendLine("- Day $day")
                    items.sortedBy { it.order }.forEach { item ->
                        appendLine("  - 이름: ${item.place?.name ?: item.title}, 주소: ${item.place?.address}")
                    }
                }
            }
            .joinToString("\n")
        val wishlistText = trip.wishlistItems
            .map { wishlistItem -> "    - 이름: ${wishlistItem.place.name}, 주소: ${wishlistItem.place.address}" }
            .ifEmpty { listOf("    - 없음") }
            .joinToString("\n")
        val encodedExampleQuery = URLEncoder.encode("킨류 라멘 도톤보리점", StandardCharsets.UTF_8)
        val reviewConcept = concept ?: "일반 여행"

        return """
        당신은 최고의 여행 전문가이자 꼼꼼한 여행 계획 검토자입니다. 주어진 [여행 정보], [상세 일정], [사용자들의 위시리스트]를 바탕으로 여행 계획을 매우 구체적이고 실용적으로 검토해야 합니다.

        반드시 아래 규칙을 따르세요.
        1. 입력에 없는 사실은 만들지 마세요.
        2. 입력에 없는 구체 날짜, 지역명, 행정구역, 상호 디테일, 실제 방문 체험, 음식 맛/향/분위기 묘사를 추가하지 마세요.
        3. 근거가 부족한 판단은 단정하지 말고 보수적으로 표현하세요.
        4. 공항은 동선 분석에서 제외하세요.
        5. 추천 장소 목록 구간에서는 장소 이름만 한 줄에 하나씩 적고, bullet, numbering, 설명, 주소, 링크, 후기 문장을 넣지 마세요.

        [단계별 지침]
        1. 계획의 유효성 검증
        - 국가 일치 여부를 먼저 확인하세요. [상세 일정]의 주소가 ${trip.country.koreanName} 여행과 맞지 않는 장소가 하나라도 있으면 즉시 리뷰를 중단하고 아래 메시지만 출력하세요.
          "계획 검토를 중단합니다. '${trip.country.koreanName}' 여행 계획에 다른 국가의 장소인 '[잘못된 장소 이름]'이 포함되어 있습니다. 전체 일정을 다시 확인하고 수정해주세요."
        - 비현실적인 이동, 과도한 일정, 여행 콘셉트('$reviewConcept')와 실제 일정의 심각한 불일치가 있는지 확인하세요.
        - 위 문제가 심각하면 상세 리뷰 전에 경고 문단으로 먼저 지적하고 수정을 제안하세요.

        2. 상세 검토 및 제안
        - 계획에 큰 문제가 없으면 날짜별로 동선 효율성, 일정 현실성, 여행 콘셉트('$reviewConcept') 적합성을 상세하게 분석하세요.
        - 추천 장소와 맛집은 여행 콘셉트('$reviewConcept')와 직접 관련되어야 하며 ${trip.country.koreanName}에 속한 곳만 추천하세요.
        - 사용자들의 위시리스트에 있는 장소를 우선 고려하세요.
        - 추천하는 장소를 본문에 언급할 때는 클릭 가능한 Google Maps 검색 링크를 사용하세요.
        - 링크 형식은 `https://www.google.com/maps/search/?api=1&query=<URL_ENCODED_PLACE_NAME>` 입니다.
        - 예시: `[킨류 라멘 金龍ラーメン](https://www.google.com/maps/search/?api=1&query=$encodedExampleQuery)`

        [출력 규칙]
        1. 국가 불일치가 발견되면 다른 문장 없이 중단 메시지만 출력하세요.
        2. 수정이 필요한 문제가 발견되면 제목을 `## ${trip.title} 수정 제안`으로 시작하세요.
        3. 계획에 큰 문제가 없으면 제목을 `## ${trip.title} 상세 검토 및 제안`으로 시작하세요.
        4. 본문이 끝난 후 반드시 줄바꿈하고 `---추천 장소 목록---` 구분자를 추가하세요.
        5. 그 다음 줄부터 본문에서 추천한 모든 장소의 이름만 한 줄에 하나씩 정확하게 나열하세요.

        [여행 정보]
        - 여행 제목: ${trip.title}
        - 여행 국가: ${trip.country.koreanName}
        - 여행 기간: ${trip.startDate} ~ ${trip.endDate}
        - 여행 콘셉트: `$reviewConcept`

        [상세 일정]
        $itineraryText

        [사용자들의 위시리스트]
        $wishlistText
        """.trimIndent()
    }
}
