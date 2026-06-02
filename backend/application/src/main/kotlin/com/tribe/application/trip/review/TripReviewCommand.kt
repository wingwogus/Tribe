package com.tribe.application.trip.review

/**
 * 여행 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object TripReviewCommand {
    data class Create(
        val tripId: Long,
        val concept: String? = null,
    )
}
