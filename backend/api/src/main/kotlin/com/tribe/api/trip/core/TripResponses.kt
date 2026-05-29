package com.tribe.api.trip.core

import com.tribe.application.trip.core.TripResult

object TripResponses {
    data class InvitationResponse(
        val inviteLink: String,
    ) {
        companion object {
            fun from(result: TripResult.Invitation) = InvitationResponse(result.inviteLink)
        }
    }

    data class MemberResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val avatar: String?,
        val role: String,
    ) {
        companion object {
            fun from(result: TripResult.MemberSummary) = MemberResponse(
                tripMemberId = result.tripMemberId,
                memberId = result.memberId,
                nickname = result.nickname,
                avatar = result.avatar,
                role = result.role,
            )
        }
    }

    data class SimpleTripResponse(
        val tripId: Long,
        val title: String,
        val startDate: java.time.LocalDate,
        val endDate: java.time.LocalDate,
        val country: String,
        val regionCode: String?,
        val memberCount: Int,
        val members: List<MemberResponse>,
    ) {
        companion object {
            fun from(result: TripResult.SimpleTrip) = SimpleTripResponse(
                tripId = result.tripId,
                title = result.title,
                startDate = result.startDate,
                endDate = result.endDate,
                country = result.country,
                regionCode = result.regionCode,
                memberCount = result.memberCount,
                members = result.members.map(MemberResponse::from),
            )
        }
    }

    data class TripDetailResponse(
        val tripId: Long,
        val title: String,
        val startDate: java.time.LocalDate,
        val endDate: java.time.LocalDate,
        val country: String,
        val regionCode: String?,
        val members: List<MemberResponse>,
    ) {
        companion object {
            fun from(result: TripResult.TripDetail) = TripDetailResponse(
                tripId = result.tripId,
                title = result.title,
                startDate = result.startDate,
                endDate = result.endDate,
                country = result.country,
                regionCode = result.regionCode,
                members = result.members.map(MemberResponse::from),
            )
        }
    }
}
