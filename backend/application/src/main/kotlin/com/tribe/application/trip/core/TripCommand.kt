package com.tribe.application.trip.core

import java.time.LocalDate

object TripCommand {
    data class Create(
        val title: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val country: String,
        val regionCode: String? = null,
    )

    data class Update(
        val tripId: Long,
        val title: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val country: String,
        val regionCode: String? = null,
    )

    data class Join(
        val token: String,
    )

    data class Import(
        val postId: Long,
        val title: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    data class AddGuest(
        val tripId: Long,
        val nickname: String,
    )

    data class DeleteGuest(
        val tripId: Long,
        val tripMemberId: Long,
    )

    data class Leave(
        val tripId: Long,
    )

    data class KickMember(
        val tripId: Long,
        val memberId: Long,
    )

    data class AssignRole(
        val tripId: Long,
        val memberId: Long,
        val role: String,
    )
}
