package com.tribe.api.trip.review

import com.tribe.application.trip.review.TripReviewResult
import java.time.LocalDateTime

object TripReviewResponses {
    data class RecommendedPlaceResponse(
        val placeId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
    )

    data class ReviewDetailResponse(
        val reviewId: Long,
        val concept: String?,
        val content: String?,
        val createdAt: LocalDateTime?,
        val recommendedPlaces: List<RecommendedPlaceResponse>,
    ) {
        companion object {
            fun from(result: TripReviewResult.ReviewDetail) = ReviewDetailResponse(
                reviewId = result.reviewId,
                concept = result.concept,
                content = result.content,
                createdAt = result.createdAt,
                recommendedPlaces = result.recommendedPlaces.map {
                    RecommendedPlaceResponse(
                        placeId = it.placeId,
                        externalPlaceId = it.externalPlaceId,
                        placeName = it.placeName,
                        address = it.address,
                        latitude = it.latitude,
                        longitude = it.longitude,
                    )
                },
            )
        }
    }

    data class SimpleReviewInfoResponse(
        val reviewId: Long,
        val title: String?,
        val concept: String?,
        val createdAt: LocalDateTime?,
    ) {
        companion object {
            fun from(result: TripReviewResult.SimpleReviewInfo) = SimpleReviewInfoResponse(
                reviewId = result.reviewId,
                title = result.title,
                concept = result.concept,
                createdAt = result.createdAt,
            )
        }
    }
}
