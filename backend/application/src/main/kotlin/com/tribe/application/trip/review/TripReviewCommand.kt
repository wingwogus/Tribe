package com.tribe.application.trip.review

object TripReviewCommand {
    data class Create(
        val tripId: Long,
        val concept: String? = null,
    )
}
