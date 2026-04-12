package com.tribe.domain.trip.member

import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Trip
import org.springframework.data.jpa.repository.JpaRepository

interface TripMemberRepository : JpaRepository<TripMember, Long> {
    fun findByTripAndMember(trip: Trip, member: Member): TripMember?
    fun existsByTripIdAndMemberId(tripId: Long, memberId: Long): Boolean
    fun findByTripIdAndMemberId(tripId: Long, memberId: Long): TripMember?
    fun findByTripIdAndRole(tripId: Long, role: TripRole): List<TripMember>
}
