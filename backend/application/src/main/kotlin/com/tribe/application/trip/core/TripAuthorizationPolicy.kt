package com.tribe.application.trip.core

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.member.TripRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("tripAuthorizationPolicy")
class TripAuthorizationPolicy(
    private val tripMemberRepository: TripMemberRepository,
    private val currentActor: CurrentActor,
) {
    @Transactional(readOnly = true)
    fun isTripMember(tripId: Long): Boolean {
        val memberId = currentActor.requireUserId()
        val tripMember = tripMemberRepository.findByTripIdAndMemberId(tripId, memberId)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)

        if (tripMember.role == TripRole.EXITED || tripMember.role == TripRole.KICKED) {
            throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
        }
        return true
    }

    @Transactional(readOnly = true)
    fun isTripOwner(tripId: Long): Boolean {
        val memberId = currentActor.requireUserId()
        val tripMember = tripMemberRepository.findByTripIdAndMemberId(tripId, memberId)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
        if (tripMember.role != TripRole.OWNER) {
            throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
        }
        return true
    }

    @Transactional(readOnly = true)
    fun isTripAdmin(tripId: Long): Boolean {
        val memberId = currentActor.requireUserId()
        val tripMember = tripMemberRepository.findByTripIdAndMemberId(tripId, memberId)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
        if (tripMember.role != TripRole.OWNER && tripMember.role != TripRole.ADMIN) {
            throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
        }
        return true
    }
}
