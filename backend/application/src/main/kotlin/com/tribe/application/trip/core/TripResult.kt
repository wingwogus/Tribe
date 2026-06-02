package com.tribe.application.trip.core

import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripRole
import java.time.LocalDate

/**
 * 여행 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object TripResult {
    data class Invitation(
        val inviteLink: String,
    )

    data class MemberSummary(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val avatar: String?,
        val role: String,
    ) {
        companion object {
            fun from(tripMember: TripMember): MemberSummary {
                return MemberSummary(
                    tripMemberId = tripMember.id,
                    memberId = tripMember.member?.id,
                    nickname = tripMember.name,
                    avatar = tripMember.member?.avatar,
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
        val regionCode: String? = null,
        val memberCount: Int,
        val members: List<MemberSummary> = emptyList(),
    ) {
        companion object {
            fun from(trip: Trip): SimpleTrip {
                val activeMembers = trip.members.filter { it.role != TripRole.KICKED && it.role != TripRole.EXITED && !it.isGuest }

                return SimpleTrip(
                    tripId = trip.id,
                    title = trip.title,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    country = trip.country.koreanName,
                    regionCode = trip.regionCode,
                    memberCount = activeMembers.size,
                    members = activeMembers.map(MemberSummary::from),
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
        val regionCode: String? = null,
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
                    regionCode = trip.regionCode,
                    members = trip.members
                        .filter { it.role != TripRole.KICKED && it.role != TripRole.EXITED }
                        .map(MemberSummary::from),
                )
            }
        }
    }
}
