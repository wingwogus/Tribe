package com.tribe.application.itinerary.item

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.OpeningHoursEvaluator
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.ItineraryAction
import com.tribe.application.trip.event.ItineraryEvent
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.core.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ItemService(
    private val itineraryItemRepository: ItineraryItemRepository,
    private val placeRepository: PlaceRepository,
    private val memberWishlistItemRepository: MemberWishlistItemRepository,
    private val placeSearchService: PlaceSearchService,
    private val placeResultAssembler: PlaceResultAssembler,
    private val openingHoursEvaluator: OpeningHoursEvaluator,
    private val currentActor: CurrentActor,
    private val tripRealtimeEventPublisher: TripRealtimeEventPublisher,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
    private val tripRepository: TripRepository,
) {
    fun createItem(command: ItemCommand.Create): ItemResult.Item {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val actorId = currentActor.requireUserId()
        val trip = tripRepository.findById(command.tripId)
            .orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val visitDay = command.visitDay
        val place = command.placeId?.let { placeId ->
            placeRepository.findById(placeId)
                .orElseThrow { BusinessException(ErrorCode.PLACE_NOT_FOUND) }
        }

        return createItemForPlace(
            trip = trip,
            tripId = command.tripId,
            visitDay = visitDay,
            place = place,
            title = command.title,
            time = command.time,
            memo = command.memo,
            actorId = actorId,
        )
    }

    fun createItemFromMemberWishlist(command: ItemCommand.CreateFromMemberWishlist): ItemResult.Item {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val actorId = currentActor.requireUserId()
        val memberWishlistItem = memberWishlistItemRepository.findByIdAndMemberIdWithPlace(
            id = command.memberWishlistItemId,
            memberId = actorId,
        ) ?: throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)
        val trip = tripRepository.findById(command.tripId)
            .orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }

        return createItemForPlace(
            trip = trip,
            tripId = command.tripId,
            visitDay = command.visitDay,
            place = memberWishlistItem.place,
            title = null,
            time = command.time,
            memo = command.memo,
            actorId = actorId,
        )
    }

    private fun createItemForPlace(
        trip: Trip,
        tripId: Long,
        visitDay: Int,
        place: Place?,
        title: String?,
        time: LocalDateTime?,
        memo: String?,
        actorId: Long,
    ): ItemResult.Item {
        if (place == null && title.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }
        val item = itineraryItemRepository.save(
            ItineraryItem(
                trip = trip,
                visitDay = visitDay,
                place = place,
                title = if (place != null) null else normalizeNullableText(title),
                time = time,
                order = itineraryItemRepository.countByTripIdAndVisitDay(tripId, visitDay) + 1,
                memo = normalizeNullableText(memo),
            ),
        )
        val result = toItem(item)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.ITINERARY,
                tripId = tripId,
                actorId = actorId,
                itinerary = ItineraryEvent(action = ItineraryAction.ITEM_CREATED, item = result),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun getItem(tripId: Long, itemId: Long): ItemResult.Item {
        tripAuthorizationPolicy.isTripMember(tripId)
        return toItem(findItem(tripId, itemId))
    }

    @Transactional(readOnly = true)
    fun getAllItems(tripId: Long, visitDay: Int?): List<ItemResult.Item> {
        tripAuthorizationPolicy.isTripMember(tripId)
        return if (visitDay != null) {
            itineraryItemRepository.findByTripIdAndVisitDayOrderByOrderAsc(tripId, visitDay)
                .map(::toItem)
        } else {
            itineraryItemRepository.findByTripIdOrderByVisitDayAndOrder(tripId).map(::toItem)
        }
    }

    fun updateItem(command: ItemCommand.Update): ItemResult.Item {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val item = findItem(command.tripId, command.itemId)
        val previousVisitDay = item.visitDay

        command.visitDay?.let { targetVisitDay ->
            if (targetVisitDay != item.visitDay) {
                item.visitDay = targetVisitDay
                item.order = itineraryItemRepository.countByTripIdAndVisitDay(command.tripId, targetVisitDay) + 1
            }
        }
        command.title?.let { item.title = normalizeNullableText(it) }
        item.time = command.time
        item.memo = normalizeNullableText(command.memo)

        val result = toItem(item)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.ITINERARY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                itinerary = ItineraryEvent(
                    action = if (previousVisitDay != item.visitDay) ItineraryAction.ITEM_MOVED_DAY else ItineraryAction.ITEM_UPDATED,
                    item = result,
                ),
            ),
        )
        return result
    }

    fun updateItemOrder(command: ItemCommand.OrderUpdate): List<ItemResult.Item> {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val newOrderMap = command.items.associateBy({ it.itemId }, { it.itemOrder })
        if (newOrderMap.size != command.items.size) throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)

        val items = itineraryItemRepository.findByIdInAndTripId(command.items.map { it.itemId }, command.tripId)
        if (items.size != command.items.size) throw BusinessException(ErrorCode.ITEM_NOT_FOUND)

        command.items.forEach { orderItem ->
            val item = items.first { it.id == orderItem.itemId }
            item.visitDay = orderItem.visitDay
            item.order = orderItem.itemOrder
        }

        val result = items.sortedWith(compareBy(ItineraryItem::visitDay, ItineraryItem::order)).map(::toItem)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.ITINERARY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                itinerary = ItineraryEvent(
                    action = ItineraryAction.ITEM_REORDERED,
                    items = result,
                    orderChanges = command.items.map {
                        ItineraryEvent.OrderChange(
                            itemId = it.itemId,
                            visitDay = it.visitDay,
                            itemOrder = it.itemOrder,
                        )
                    },
                ),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun getAllDirectionsForTrip(tripId: Long, mode: String): List<RouteDetails> {
        tripAuthorizationPolicy.isTripMember(tripId)
        val items = itineraryItemRepository.findByTripIdOrderByVisitDayAndOrder(tripId)
        if (items.size < 2) return emptyList()

        return items.zipWithNext().mapNotNull { (originItem, destinationItem) ->
            val originPlace = originItem.place ?: return@mapNotNull null
            val destinationPlace = destinationItem.place ?: return@mapNotNull null
            placeSearchService.directions(originPlace.externalPlaceId, destinationPlace.externalPlaceId, mode)
        }
    }

    fun deleteItem(tripId: Long, itemId: Long) {
        tripAuthorizationPolicy.isTripMember(tripId)
        itineraryItemRepository.delete(findItem(tripId, itemId))
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.ITINERARY,
                tripId = tripId,
                actorId = currentActor.requireUserId(),
                itinerary = ItineraryEvent(action = ItineraryAction.ITEM_DELETED, deletedItemId = itemId),
            ),
        )
    }

    private fun findItem(tripId: Long, itemId: Long): ItineraryItem {
        val item = itineraryItemRepository.findById(itemId)
            .orElseThrow { BusinessException(ErrorCode.ITEM_NOT_FOUND) }
        if (item.trip.id != tripId) {
            throw BusinessException(ErrorCode.NO_BELONG_TRIP)
        }
        return item
    }

    private fun toItem(item: ItineraryItem): ItemResult.Item {
        val placeTypeSummary = placeResultAssembler.toPlaceTypeSummary(item.place)
        val photoHint = placeResultAssembler.toPhotoHint(item.place)?.let { ItemResult.PhotoHint(it.name, it.photoUri) }
        val placeDetailSummary = placeResultAssembler.toDetailSummary(item.place)
        val openingStatus = if (item.place != null) {
            openingHoursEvaluator.evaluate(item, item.trip.startDate)
        } else {
            null
        }
        return ItemResult.Item.from(item, placeTypeSummary, photoHint, placeDetailSummary, openingStatus)
    }

    private fun normalizeNullableText(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }
}
