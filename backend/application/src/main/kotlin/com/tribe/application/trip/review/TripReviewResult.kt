package com.tribe.application.trip.review

import com.tribe.domain.trip.review.TripReview
import java.time.LocalDateTime

object TripReviewResult {
    data class ReviewDetail(
        val reviewId: Long,
        val concept: String?,
        val content: String?,
        val createdAt: LocalDateTime?,
        val recommendedPlaces: List<RecommendedPlaceResult>,
    ) {
        companion object {
            fun from(review: TripReview): ReviewDetail {
                return ReviewDetail(
                    reviewId = review.id,
                    concept = review.concept,
                    content = review.content,
                    createdAt = review.createdAt,
                    recommendedPlaces = review.recommendedPlaces.map {
                        RecommendedPlaceResult(
                            placeId = it.place.id,
                            externalPlaceId = it.place.externalPlaceId,
                            placeName = it.place.name,
                            address = it.place.address,
                            latitude = it.place.latitude.toDouble(),
                            longitude = it.place.longitude.toDouble(),
                        )
                    },
                )
            }
        }
    }

    data class SimpleReviewInfo(
        val reviewId: Long,
        val title: String?,
        val concept: String?,
        val createdAt: LocalDateTime?,
    ) {
        companion object {
            fun from(review: TripReview): SimpleReviewInfo {
                return SimpleReviewInfo(
                    reviewId = review.id,
                    title = extractTitle(review.content),
                    concept = review.concept,
                    createdAt = review.createdAt,
                )
            }

            private fun extractTitle(text: String): String? {
                return if (text.startsWith("## ")) {
                    text.substringAfter("## ").substringBefore("\n")
                } else null
            }
        }
    }

    data class RecommendedPlaceResult(
        val placeId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
    )
}
