package com.tribe.domain.trip.member

import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Trip
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 회원 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface TripMemberRepository : JpaRepository<TripMember, Long> {
    fun findByTripAndMember(trip: Trip, member: Member): TripMember?
    fun existsByTripIdAndMemberId(tripId: Long, memberId: Long): Boolean
    fun findByTripIdAndMemberId(tripId: Long, memberId: Long): TripMember?
    fun findByTripIdAndRole(tripId: Long, role: TripRole): List<TripMember>
}
