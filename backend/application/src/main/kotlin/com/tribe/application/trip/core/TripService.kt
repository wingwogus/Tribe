package com.tribe.application.trip.core

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.redis.TripInvitationRepository
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.member.TripMemberIntegrityService
import com.tribe.application.trip.event.TripLifecycleAction
import com.tribe.application.trip.event.TripLifecycleEvent
import com.tribe.application.trip.event.TripMemberAction
import com.tribe.application.trip.event.TripMemberEvent
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.application.trip.event.TripSummary
import com.tribe.domain.community.CommunityPostRepository
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.trip.core.Country
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

@Service
@Transactional
class TripService(
    private val currentActor: CurrentActor,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
    private val tripMemberIntegrityService: TripMemberIntegrityService,
    private val tripRealtimeEventPublisher: TripRealtimeEventPublisher,
    private val memberRepository: MemberRepository,
    private val tripRepository: TripRepository,
    private val tripMemberRepository: TripMemberRepository,
    private val tripInvitationRepository: TripInvitationRepository,
    private val communityPostRepository: CommunityPostRepository,
    @Value("\${app.url}") private val appUrl: String,
) {
    companion object {
        private const val INVITE_PATH = "/invite?token="
        private val INVITE_EXPIRATION = Duration.ofDays(7)
    }

    fun createTrip(command: TripCommand.Create): TripResult.TripDetail {
        val member = findCurrentMember()
        val trip = Trip(
            title = command.title,
            startDate = command.startDate,
            endDate = command.endDate,
            country = Country.from(command.country),
        ).apply {
            addMember(member, TripRole.OWNER)
        }
        val result = TripResult.TripDetail.from(tripRepository.save(trip))
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_LIFECYCLE,
                tripId = result.tripId,
                actorId = currentActor.requireUserId(),
                lifecycle = TripLifecycleEvent(action = TripLifecycleAction.CREATED, trip = TripSummary.from(result)),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun getTripDetails(tripId: Long): TripResult.TripDetail {
        val currentMemberId = currentActor.requireUserId()
        val trip = findTripWithMembers(tripId)
        val membership = trip.members.firstOrNull { it.member?.id == currentMemberId }
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
        if (membership.role == TripRole.EXITED || membership.role == TripRole.KICKED) {
            throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
        }
        val result = TripResult.TripDetail.from(trip)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_LIFECYCLE,
                tripId = result.tripId,
                actorId = currentActor.requireUserId(),
                lifecycle = TripLifecycleEvent(action = TripLifecycleAction.UPDATED, trip = TripSummary.from(result)),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun getAllTrips(pageable: Pageable): Page<TripResult.SimpleTrip> {
        val currentMemberId = currentActor.requireUserId()
        return tripRepository.findTripsByMemberId(currentMemberId, pageable).map(TripResult.SimpleTrip::from)
    }

    fun updateTrip(command: TripCommand.Update): TripResult.TripDetail {
        tripAuthorizationPolicy.isTripOwner(command.tripId)
        val trip = findTripWithMembers(command.tripId)
        trip.update(command.title, command.startDate, command.endDate, Country.from(command.country))
        return TripResult.TripDetail.from(trip)
    }

    fun deleteTrip(tripId: Long) {
        tripAuthorizationPolicy.isTripOwner(tripId)
        val trip = tripRepository.findById(tripId).orElseThrow {
            BusinessException(ErrorCode.TRIP_NOT_FOUND)
        }
        tripRepository.delete(trip)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_LIFECYCLE,
                tripId = tripId,
                actorId = currentActor.requireUserId(),
                lifecycle = TripLifecycleEvent(action = TripLifecycleAction.DELETED, deletedTripId = tripId),
            ),
        )
    }

    fun createInvitation(tripId: Long): TripResult.Invitation {
        tripAuthorizationPolicy.isTripAdmin(tripId)
        if (!tripRepository.existsById(tripId)) {
            throw BusinessException(ErrorCode.TRIP_NOT_FOUND)
        }
        val token = generateInvitationToken()
        tripInvitationRepository.save(token, tripId, INVITE_EXPIRATION)
        return TripResult.Invitation("$appUrl$INVITE_PATH$token")
    }

    fun joinTrip(command: TripCommand.Join): TripResult.TripDetail {
        val currentMemberId = currentActor.requireUserId()
        val tripId = tripInvitationRepository.getTripId(command.token)
            ?: throw BusinessException(ErrorCode.INVALID_INVITE_TOKEN)
        val trip = findTripWithMembers(tripId)
        val existingMember = tripMemberRepository.findByTripIdAndMemberId(tripId, currentMemberId)

        when {
            existingMember == null -> {
                val member = findCurrentMember()
                trip.addMember(member, TripRole.MEMBER)
            }
            existingMember.role == TripRole.MEMBER || existingMember.role == TripRole.OWNER -> {
                throw BusinessException(ErrorCode.ALREADY_JOINED_TRIP)
            }
            existingMember.role == TripRole.KICKED -> {
                throw BusinessException(ErrorCode.BANNED_MEMBER)
            }
            existingMember.role == TripRole.EXITED -> {
                existingMember.role = TripRole.MEMBER
            }
            else -> Unit
        }

        val joinedMembership = trip.members.firstOrNull {
            it.member?.id == currentMemberId && it.role != TripRole.EXITED && it.role != TripRole.KICKED
        } ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)

        val result = TripResult.TripDetail.from(trip)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_MEMBER,
                tripId = result.tripId,
                actorId = currentMemberId,
                member = TripMemberEvent(action = TripMemberAction.MEMBER_JOINED, member = TripResult.MemberSummary.from(joinedMembership)),
            ),
        )
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_LIFECYCLE,
                tripId = result.tripId,
                actorId = currentMemberId,
                lifecycle = TripLifecycleEvent(action = TripLifecycleAction.UPDATED, trip = TripSummary.from(result)),
            ),
        )
        return result
    }

    fun importTrip(command: TripCommand.Import): TripResult.TripDetail {
        val member = findCurrentMember()
        val post = communityPostRepository.findById(command.postId)
            .orElseThrow { BusinessException(ErrorCode.POST_NOT_FOUND) }
        val originalTrip = tripRepository.findTripWithFullItineraryById(post.trip.id)
            ?: throw BusinessException(ErrorCode.TRIP_NOT_FOUND)

        val importTrip = Trip(
            title = command.title,
            startDate = command.startDate,
            endDate = command.endDate,
            country = originalTrip.country,
        )
        importTrip.addMember(member, TripRole.OWNER)

        originalTrip.categories.forEach { originalCategory ->
            val importCategory = Category(
                trip = importTrip,
                day = originalCategory.day,
                name = originalCategory.name,
                order = originalCategory.order,
            ).also {
                it.memo = originalCategory.memo
            }

            originalCategory.itineraryItems.mapTo(importCategory.itineraryItems) { originalItem ->
                ItineraryItem(
                    category = importCategory,
                    place = originalItem.place,
                    title = originalItem.title,
                    time = originalItem.time,
                    order = originalItem.order,
                    memo = originalItem.memo,
                )
            }

            importTrip.categories.add(importCategory)
        }

        val result = TripResult.TripDetail.from(tripRepository.save(importTrip))
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_LIFECYCLE,
                tripId = result.tripId,
                actorId = currentActor.requireUserId(),
                lifecycle = TripLifecycleEvent(action = TripLifecycleAction.IMPORTED, trip = TripSummary.from(result)),
            ),
        )
        return result
    }

    fun addGuest(command: TripCommand.AddGuest): TripResult.TripDetail {
        tripAuthorizationPolicy.isTripAdmin(command.tripId)
        val trip = findTripWithMembers(command.tripId)
        val guest = TripMember(
                member = null,
                trip = trip,
                guestNickname = command.nickname.trim(),
                role = TripRole.GUEST,
            )
        trip.members.add(guest)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.TRIP_MEMBER,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                member = TripMemberEvent(action = TripMemberAction.GUEST_ADDED, member = TripResult.MemberSummary.from(guest)),
            ),
        )
        return TripResult.TripDetail.from(trip)
    }

    fun deleteGuest(command: TripCommand.DeleteGuest): TripResult.TripDetail {
        return tripMemberIntegrityService.deleteGuest(command)
    }

    fun leaveTrip(command: TripCommand.Leave): TripResult.TripDetail {
        return tripMemberIntegrityService.leaveTrip(command)
    }

    fun kickMember(command: TripCommand.KickMember): TripResult.TripDetail {
        return tripMemberIntegrityService.kickMember(command)
    }

    fun assignRole(command: TripCommand.AssignRole): TripResult.TripDetail {
        return tripMemberIntegrityService.assignRole(command)
    }

    private fun findCurrentMember() =
        memberRepository.findById(currentActor.requireUserId()).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }

    private fun findTripWithMembers(tripId: Long): Trip =
        tripRepository.findTripWithMembersById(tripId)
            ?: throw BusinessException(ErrorCode.TRIP_NOT_FOUND)

    private fun generateInvitationToken(): String {
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }
}
