package com.tribe.api.trip.review

import com.tribe.application.trip.review.TripReviewCommand

/**
 * 여행 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object TripReviewRequests {
    data class CreateReviewRequest(
        val concept: String? = null,
    ) {
        fun toCommand(tripId: Long): TripReviewCommand.Create = TripReviewCommand.Create(
            tripId = tripId,
            concept = concept,
        )
    }
}
