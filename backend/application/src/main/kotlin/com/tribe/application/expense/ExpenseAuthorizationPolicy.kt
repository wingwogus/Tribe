package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.domain.expense.Expense
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.springframework.stereotype.Component

@Component
class ExpenseAuthorizationPolicy(
    private val currentActor: CurrentActor,
    private val tripRepository: TripRepository,
) {
    fun requireMembership(tripId: Long): TripMember {
        val actorId = currentActor.requireUserId()
        val trip = tripRepository.findTripWithMembersById(tripId)
            ?: throw BusinessException(ErrorCode.TRIP_NOT_FOUND)

        return trip.members.firstOrNull {
            it.member?.id == actorId && it.role != TripRole.EXITED && it.role != TripRole.KICKED
        } ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
    }

    fun requireModificationAccess(expense: Expense): TripMember {
        val membership = requireMembership(expense.trip.id)
        val actorId = membership.member?.id ?: throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
        if (expense.createdBy.id == actorId) {
            return membership
        }
        if (membership.role == TripRole.OWNER || membership.role == TripRole.ADMIN) {
            return membership
        }
        throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
    }
}
