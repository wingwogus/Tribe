package com.tribe.api.trip.review

object TripReviewRequests {
    data class CreateReviewRequest(
        val concept: String? = null,
    )
}
