package com.tribe.application.trip.core

import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripRole
import java.time.LocalDate

object TripResult {
    data class Invitation(
        val inviteLink: String,
    )

    data class MemberSummary(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val role: String,
    ) {
        companion object {
            fun from(tripMember: TripMember): MemberSummary {
                return MemberSummary(
                    tripMemberId = tripMember.id,
                    memberId = tripMember.member?.id,
                    nickname = tripMember.name,
                    role = tripMember.role.name,
                )
            }
        }
    }

    data class SimpleTrip(
        val tripId: Long,
        val title: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val country: String,
        val memberCount: Int,
    ) {
        companion object {
            fun from(trip: Trip): SimpleTrip {
                return SimpleTrip(
                    tripId = trip.id,
                    title = trip.title,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    country = trip.country.koreanName,
                    memberCount = trip.members.count { it.role != TripRole.KICKED && it.role != TripRole.EXITED && !it.isGuest },
                )
            }
        }
    }

    data class TripDetail(
        val tripId: Long,
        val title: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val country: String,
        val members: List<MemberSummary>,
    ) {
        companion object {
            fun from(trip: Trip): TripDetail {
                return TripDetail(
                    tripId = trip.id,
                    title = trip.title,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    country = trip.country.code,
                    members = trip.members
                        .filter { it.role != TripRole.KICKED && it.role != TripRole.EXITED }
                        .map(MemberSummary::from),
                )
            }
        }
    }
}
