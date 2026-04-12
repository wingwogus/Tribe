package com.tribe.application.trip.member

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.expense.ExpenseCalculator
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.application.trip.core.TripCommand
import com.tribe.application.trip.core.TripResult
import com.tribe.application.trip.event.TripMemberAction
import com.tribe.application.trip.event.TripMemberEvent
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.domain.expense.ExpenseAssignment
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TripMemberIntegrityService(
    private val currentActor: CurrentActor,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
    private val tripRealtimeEventPublisher: TripRealtimeEventPublisher,
    private val tripRepository: TripRepository,
    private val expenseRepository: ExpenseRepository,
    private val wishlistItemRepository: WishlistItemRepository,
) {
    fun deleteGuest(command: TripCommand.DeleteGuest): TripResult.TripDetail {
        tripAuthorizationPolicy.isTripAdmin(command.tripId)
        val trip = findTripWithMembers(command.tripId)
        val guest = trip.members.firstOrNull { it.id == command.tripMemberId && it.role == TripRole.GUEST }
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

        val owner = trip.members.firstOrNull { it.role == TripRole.OWNER }
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, detail = mapOf("reason" to "owner_not_found"))

        val expenses = expenseRepository.findAllWithDetailsByTripId(command.tripId)
        expenses.filter { it.payer.id == guest.id }.forEach { expense ->
            expense.payer = owner
        }

        val affectedItems = expenses
            .flatMap { it.expenseItems }
            .filter { item -> item.assignments.any { assignment -> assignment.tripMember.id == guest.id } }
            .distinctBy { it.id }

        affectedItems.forEach { item ->
            val remainingMembers = item.assignments
                .filter { it.tripMember.id != guest.id }
                .map { it.tripMember }

            if (remainingMembers.isNotEmpty()) {
                val amounts = ExpenseCalculator.calculateFairShare(item.price, remainingMembers.size)
                item.replaceAssignments(
                    remainingMembers.zip(amounts).map { (member, amount) ->
                        ExpenseAssignment(
                            expenseItem = item,
                            tripMember = member,
                            amount = amount,
                        )
                    },
                )
            } else {
                item.replaceAssignments(
                    listOf(
                        ExpenseAssignment(
                            expenseItem = item,
                            tripMember = item.expense.payer,
                            amount = item.price,
                        ),
                    ),
                )
            }
        }

        wishlistItemRepository.deleteByAdderId(guest.id)
        trip.members.remove(guest)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_MEMBER,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                member = TripMemberEvent(action = TripMemberAction.GUEST_DELETED, member = TripResult.MemberSummary.from(guest)),
            ),
        )
        return TripResult.TripDetail.from(trip)
    }

    fun leaveTrip(command: TripCommand.Leave): TripResult.TripDetail {
        val actorId = currentActor.requireUserId()
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val trip = findTripWithMembers(command.tripId)
        val membership = findActiveMembership(trip, actorId)
        if (membership.role == TripRole.OWNER) {
            throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
        }
        wishlistItemRepository.deleteByAdderId(membership.id)
        membership.role = TripRole.EXITED
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_MEMBER,
                tripId = command.tripId,
                actorId = actorId,
                member = TripMemberEvent(action = TripMemberAction.MEMBER_LEFT, member = TripResult.MemberSummary.from(membership)),
            ),
        )
        return TripResult.TripDetail.from(trip)
    }

    fun kickMember(command: TripCommand.KickMember): TripResult.TripDetail {
        tripAuthorizationPolicy.isTripAdmin(command.tripId)
        val actorId = currentActor.requireUserId()
        if (actorId == command.memberId) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        val trip = findTripWithMembers(command.tripId)
        val actorMembership = findActiveMembership(trip, actorId)
        val targetMembership = findActiveMembership(trip, command.memberId)

        if (targetMembership.role == TripRole.OWNER) {
            throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
        }
        if (actorMembership.role == TripRole.ADMIN && targetMembership.role != TripRole.MEMBER) {
            throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
        }

        wishlistItemRepository.deleteByAdderId(targetMembership.id)
        targetMembership.role = TripRole.KICKED
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_MEMBER,
                tripId = command.tripId,
                actorId = actorId,
                member = TripMemberEvent(action = TripMemberAction.MEMBER_KICKED, member = TripResult.MemberSummary.from(targetMembership)),
            ),
        )
        return TripResult.TripDetail.from(trip)
    }

    fun assignRole(command: TripCommand.AssignRole): TripResult.TripDetail {
        tripAuthorizationPolicy.isTripOwner(command.tripId)
        val trip = findTripWithMembers(command.tripId)
        val targetMembership = findActiveMembership(trip, command.memberId)
        val targetRole = parseAssignableRole(command.role)

        if (targetMembership.role == TripRole.OWNER) {
            throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP)
        }

        targetMembership.role = targetRole
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_MEMBER,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                member = TripMemberEvent(action = TripMemberAction.ROLE_CHANGED, member = TripResult.MemberSummary.from(targetMembership)),
            ),
        )
        return TripResult.TripDetail.from(trip)
    }

    private fun findTripWithMembers(tripId: Long): Trip =
        tripRepository.findTripWithMembersById(tripId)
            ?: throw BusinessException(ErrorCode.TRIP_NOT_FOUND)

    private fun findActiveMembership(trip: Trip, memberId: Long): TripMember =
        trip.members.firstOrNull {
            it.member?.id == memberId && it.role != TripRole.EXITED && it.role != TripRole.KICKED
        } ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)

    private fun parseAssignableRole(role: String): TripRole =
        when (role.trim().uppercase()) {
            TripRole.ADMIN.name -> TripRole.ADMIN
            TripRole.MEMBER.name -> TripRole.MEMBER
            else -> throw BusinessException(ErrorCode.INVALID_INPUT)
        }
}
