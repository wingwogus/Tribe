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
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.itinerary.wishlist.TripWishlistSort
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.itinerary.wishlist.WishlistItemLike
import com.tribe.domain.itinerary.wishlist.WishlistItemLikeRepository
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripMember
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WishlistService(
    private val wishlistItemRepository: WishlistItemRepository,
    private val memberWishlistItemRepository: MemberWishlistItemRepository,
    private val wishlistItemLikeRepository: WishlistItemLikeRepository,
    private val placeCatalogService: com.tribe.application.itinerary.place.PlaceCatalogService,
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
    fun searchWishList(
        tripId: Long,
        query: String,
        pageable: Pageable,
        sort: String? = null,
    ): WishlistResult.SearchPage {
        tripAuthorizationPolicy.isTripMember(tripId)
        val tripMember = findCurrentTripMember(tripId)
        val page = wishlistItemRepository.findPageByTrip(tripId, query, parseSort(sort), pageable)
        return toSearchPage(page, tripMember.id)
    }

    @Transactional(readOnly = true)
    fun getWishList(
        tripId: Long,
        pageable: Pageable,
        sort: String? = null,
    ): WishlistResult.SearchPage {
        tripAuthorizationPolicy.isTripMember(tripId)
        val tripMember = findCurrentTripMember(tripId)
        val page = wishlistItemRepository.findPageByTrip(tripId, null, parseSort(sort), pageable)
        return toSearchPage(page, tripMember.id)
    }

    fun likeWishlistItem(command: WishlistCommand.Like): WishlistResult.LikeSummary {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val tripMember = findCurrentTripMember(command.tripId)
        val wishlistItem = wishlistItemRepository.findByIdAndTripId(command.wishlistItemId, command.tripId)
            ?: throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)

        if (wishlistItemLikeRepository.existsByWishlistItem_IdAndTripMember_Id(wishlistItem.id, tripMember.id)) {
            throw BusinessException(ErrorCode.WISHLIST_ITEM_LIKE_ALREADY_EXISTS)
        }

        try {
            wishlistItemLikeRepository.saveAndFlush(WishlistItemLike(wishlistItem, tripMember))
        } catch (_: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.WISHLIST_ITEM_LIKE_ALREADY_EXISTS)
        }
        return toLikeSummary(wishlistItem.id, likedByMe = true)
    }

    fun unlikeWishlistItem(command: WishlistCommand.Like): WishlistResult.LikeSummary {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val tripMember = findCurrentTripMember(command.tripId)
        wishlistItemRepository.findByIdAndTripId(command.wishlistItemId, command.tripId)
            ?: throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)

        wishlistItemLikeRepository.deleteByWishlistItemIdAndTripMemberId(command.wishlistItemId, tripMember.id)
        return toLikeSummary(command.wishlistItemId, likedByMe = false)
    }

    private fun toSearchPage(
        page: Page<WishlistItem>,
        currentTripMemberId: Long,
    ): WishlistResult.SearchPage {
        val itemIds = page.content.map { it.id }
        val likeCounts = if (itemIds.isEmpty()) {
            emptyMap()
        } else {
            wishlistItemLikeRepository.countByWishlistItemIds(itemIds)
                .associate { it.wishlistItemId to it.likeCount }
        }
        val likedItemIds = if (itemIds.isEmpty()) {
            emptySet()
        } else {
            wishlistItemLikeRepository.findLikedWishlistItemIds(currentTripMemberId, itemIds).toSet()
        }

        return WishlistResult.SearchPage(
            content = page.content.map { item ->
                WishlistResult.Item.from(
                    entity = item,
                    likeCount = likeCounts[item.id] ?: 0L,
                    likedByMe = item.id in likedItemIds,
                )
            },
            pageNumber = page.number,
            pageSize = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            isLast = page.isLast,
        )
    }

    private fun findCurrentTripMember(tripId: Long): TripMember {
        val memberId = currentActor.requireUserId()
        return tripMemberRepository.findByTripIdAndMemberId(tripId, memberId)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)
    }

    private fun toLikeSummary(
        wishlistItemId: Long,
        likedByMe: Boolean,
    ): WishlistResult.LikeSummary =
        WishlistResult.LikeSummary(
            likeCount = wishlistItemLikeRepository.countByWishlistItem_Id(wishlistItemId),
            likedByMe = likedByMe,
        )

    private fun parseSort(sort: String?): TripWishlistSort? =
        TripWishlistSort.fromApiValue(sort)
            ?: sort?.trim()?.takeIf { it.isNotEmpty() }?.let {
                throw BusinessException(
                    errorCode = ErrorCode.INVALID_INPUT,
                    detail = mapOf("field" to "sort", "rejectedValue" to sort),
                )
            }

    fun deleteWishlistItems(command: WishlistCommand.Delete) {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val ids = command.wishlistItemIds.distinct()
        if (ids.isEmpty()) return
        val existingIds = wishlistItemRepository.findIdsByTripIdAndIdIn(command.tripId, ids)
        val missing = ids.filterNot { it in existingIds }
        if (missing.isNotEmpty()) throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)
        wishlistItemLikeRepository.deleteByWishlistItemIds(existingIds)
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
