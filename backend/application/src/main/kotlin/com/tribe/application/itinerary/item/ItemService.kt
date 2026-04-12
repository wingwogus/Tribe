package com.tribe.application.itinerary.item

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.ItineraryAction
import com.tribe.application.trip.event.ItineraryEvent
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.category.CategoryRepository
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.itinerary.place.PlaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ItemService(
    private val categoryRepository: CategoryRepository,
    private val itineraryItemRepository: ItineraryItemRepository,
    private val placeRepository: PlaceRepository,
    private val placeSearchService: PlaceSearchService,
    private val currentActor: CurrentActor,
    private val tripRealtimeEventPublisher: TripRealtimeEventPublisher,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
) {
    fun createItem(command: ItemCommand.Create): ItemResult.ItemView {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val category = findCategory(command.tripId, command.categoryId)
        val place = command.placeId?.let { placeId ->
            placeRepository.findById(placeId)
                .orElseThrow { BusinessException(ErrorCode.PLACE_NOT_FOUND) }
        }
        if (place == null && command.title.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }
        val item = itineraryItemRepository.save(
            ItineraryItem(
                category = category,
                place = place,
                title = if (place != null) null else normalizeNullableText(command.title),
                time = command.time,
                order = itineraryItemRepository.countByCategoryId(category.id) + 1,
                memo = normalizeNullableText(command.memo),
            ),
        )
        val result = ItemResult.ItemView.from(item)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.ITINERARY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                itinerary = ItineraryEvent(action = ItineraryAction.CREATED, item = result),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun getItem(tripId: Long, itemId: Long): ItemResult.ItemView {
        tripAuthorizationPolicy.isTripMember(tripId)
        return ItemResult.ItemView.from(findItem(tripId, itemId))
    }

    @Transactional(readOnly = true)
    fun getAllItems(tripId: Long, categoryId: Long?): List<ItemResult.ItemView> {
        tripAuthorizationPolicy.isTripMember(tripId)
        return if (categoryId != null) {
            val category = findCategory(tripId, categoryId)
            itineraryItemRepository.findByCategoryIdOrderByOrderAsc(category.id)
                .map(ItemResult.ItemView::from)
        } else {
            categoryRepository.findAllByTripIdOrderByDayAscOrderAsc(tripId)
                .flatMap { category ->
                    itineraryItemRepository.findByCategoryIdOrderByOrderAsc(category.id)
                        .map(ItemResult.ItemView::from)
                }
        }
    }

    fun updateItem(command: ItemCommand.Update): ItemResult.ItemView {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val item = findItem(command.tripId, command.itemId)
        val targetCategory = command.categoryId?.let { findCategory(command.tripId, it) }

        if (targetCategory != null && targetCategory.id != item.category.id) {
            item.category = targetCategory
            item.order = itineraryItemRepository.countByCategoryId(targetCategory.id) + 1
        }
        command.title?.let { item.title = normalizeNullableText(it) }
        command.time?.let { item.time = it }
        command.memo?.let { item.memo = normalizeNullableText(it) }

        val result = ItemResult.ItemView.from(item)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.ITINERARY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                itinerary = ItineraryEvent(action = ItineraryAction.UPDATED, item = result),
            ),
        )
        return result
    }

    fun updateItemOrder(command: ItemCommand.OrderUpdate): List<ItemResult.ItemView> {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val newOrderMap = command.items.associateBy({ it.itemId }, { it.order })
        if (newOrderMap.size != command.items.size) throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)

        val items = itineraryItemRepository.findByIdInAndTripId(command.items.map { it.itemId }, command.tripId)
        if (items.size != command.items.size) throw BusinessException(ErrorCode.ITEM_NOT_FOUND)

        val categoryIds = command.items.map { it.categoryId }.distinct()
        val categoryMap = categoryRepository.findAllById(categoryIds).associateBy { it.id }
        if (categoryMap.size != categoryIds.size || categoryMap.values.any { it.trip.id != command.tripId }) {
            throw BusinessException(ErrorCode.CATEGORY_NOT_FOUND)
        }

        command.items.forEach { orderItem ->
            val item = items.first { it.id == orderItem.itemId }
            val category = categoryMap[orderItem.categoryId] ?: throw BusinessException(ErrorCode.CATEGORY_NOT_FOUND)
            item.category = category
            item.order = orderItem.order
        }

        val result = items.sortedBy { it.order }.map(ItemResult.ItemView::from)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.ITINERARY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                itinerary = ItineraryEvent(action = ItineraryAction.REORDERED, items = result),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun getAllDirectionsForTrip(tripId: Long, mode: String): List<RouteDetails> {
        tripAuthorizationPolicy.isTripMember(tripId)
        val items = itineraryItemRepository.findByTripIdOrderByCategoryAndOrder(tripId)
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
                itinerary = ItineraryEvent(action = ItineraryAction.DELETED, deletedItemId = itemId),
            ),
        )
    }

    private fun findCategory(tripId: Long, categoryId: Long): Category {
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }
        if (category.trip.id != tripId) {
            throw BusinessException(ErrorCode.NO_BELONG_TRIP)
        }
        return category
    }

    private fun findItem(tripId: Long, itemId: Long): ItineraryItem {
        val item = itineraryItemRepository.findById(itemId)
            .orElseThrow { BusinessException(ErrorCode.ITEM_NOT_FOUND) }
        if (item.category.trip.id != tripId) {
            throw BusinessException(ErrorCode.NO_BELONG_TRIP)
        }
        return item
    }

    private fun normalizeNullableText(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }
}
