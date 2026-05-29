package com.tribe.application.itinerary.wishlist

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.application.trip.event.WishlistAction
import com.tribe.application.trip.event.WishlistEvent
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripMember
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WishlistService(
    private val wishlistItemRepository: WishlistItemRepository,
    private val memberWishlistItemRepository: MemberWishlistItemRepository,
    private val placeCatalogService: com.tribe.application.itinerary.place.PlaceCatalogService,
    private val placeRepository: PlaceRepository,
    private val tripMemberRepository: TripMemberRepository,
    private val tripRepository: TripRepository,
    private val memberRepository: MemberRepository,
    private val currentActor: CurrentActor,
    private val tripRealtimeEventPublisher: TripRealtimeEventPublisher,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
) {
    fun addWishList(command: WishlistCommand.Add): WishlistResult.Item {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val memberId = currentActor.requireUserId()
        val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val trip = tripRepository.findById(command.tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val tripMember = tripMemberRepository.findByTripAndMember(trip, member)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)

        ensurePlaceNotInWishList(command.tripId, command.externalPlaceId)

        val place = placeCatalogService.getOrCreateAndEnrich(
            externalPlaceId = command.externalPlaceId,
            placeName = command.placeName,
            address = command.address,
            latitude = command.latitude,
            longitude = command.longitude,
        )

        return addWishListPlace(command.tripId, trip, tripMember, place, memberId)
    }

    fun addWishListFromMemberWishlist(command: WishlistCommand.AddFromMemberWishlist): WishlistResult.Item {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val memberId = currentActor.requireUserId()
        val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val trip = tripRepository.findById(command.tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val tripMember = tripMemberRepository.findByTripAndMember(trip, member)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
        val memberWishlistItem = memberWishlistItemRepository.findByIdAndMemberIdWithPlace(
            id = command.memberWishlistItemId,
            memberId = memberId,
        ) ?: throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)

        ensurePlaceNotInWishList(command.tripId, memberWishlistItem.place.externalPlaceId)
        return addWishListPlace(command.tripId, trip, tripMember, memberWishlistItem.place, memberId)
    }

    fun addWishListFromPlace(command: WishlistCommand.AddFromPlace): WishlistResult.Item {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val memberId = currentActor.requireUserId()
        val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val trip = tripRepository.findById(command.tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val tripMember = tripMemberRepository.findByTripAndMember(trip, member)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
        val place = placeRepository.findById(command.placeId)
            .orElseThrow { BusinessException(ErrorCode.PLACE_NOT_FOUND) }

        ensurePlaceNotInWishList(command.tripId, place.externalPlaceId)
        return addWishListPlace(command.tripId, trip, tripMember, place, memberId)
    }

    private fun ensurePlaceNotInWishList(tripId: Long, externalPlaceId: String) {
        if (wishlistItemRepository.existsByTrip_IdAndPlace_ExternalPlaceId(tripId, externalPlaceId)) {
            throw BusinessException(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS)
        }
    }

    private fun addWishListPlace(
        tripId: Long,
        trip: Trip,
        tripMember: TripMember,
        place: Place,
        actorId: Long,
    ): WishlistResult.Item {
        val saved = wishlistItemRepository.save(
            WishlistItem(
                trip = trip,
                place = place,
                adder = tripMember,
            )
        )
        val result = WishlistResult.Item.from(saved)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.WISHLIST,
                tripId = tripId,
                actorId = actorId,
                wishlist = WishlistEvent(action = WishlistAction.ADDED, item = result),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun searchWishList(tripId: Long, query: String, pageable: Pageable): WishlistResult.SearchPage {
        tripAuthorizationPolicy.isTripMember(tripId)
        val page = wishlistItemRepository.findAllByTrip_IdAndPlace_NameContainingIgnoreCase(tripId, query, pageable)
        return WishlistResult.SearchPage(
            content = page.content.map(WishlistResult.Item::from),
            pageNumber = page.number,
            pageSize = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            isLast = page.isLast,
        )
    }

    @Transactional(readOnly = true)
    fun getWishList(tripId: Long, pageable: Pageable): WishlistResult.SearchPage {
        tripAuthorizationPolicy.isTripMember(tripId)
        val page = wishlistItemRepository.findAllByTrip_Id(tripId, pageable)
        return WishlistResult.SearchPage(
            content = page.content.map(WishlistResult.Item::from),
            pageNumber = page.number,
            pageSize = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            isLast = page.isLast,
        )
    }

    fun deleteWishlistItems(command: WishlistCommand.Delete) {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val ids = command.wishlistItemIds.distinct()
        if (ids.isEmpty()) return
        val existingIds = wishlistItemRepository.findIdsByTripIdAndIdIn(command.tripId, ids)
        val missing = ids.filterNot { it in existingIds }
        if (missing.isNotEmpty()) throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)
        wishlistItemRepository.deleteAllByIdInBatch(existingIds)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.WISHLIST,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                wishlist = WishlistEvent(action = WishlistAction.DELETED, deletedItemIds = existingIds),
            ),
        )
    }
}
