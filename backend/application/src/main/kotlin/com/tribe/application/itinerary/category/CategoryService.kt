package com.tribe.application.itinerary.category

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.CategoryAction
import com.tribe.application.trip.event.CategoryEvent
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.category.CategoryRepository
import com.tribe.domain.trip.core.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val tripRepository: TripRepository,
    private val currentActor: CurrentActor,
    private val tripRealtimeEventPublisher: TripRealtimeEventPublisher,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
) {
    fun createCategory(command: CategoryCommand.Create): CategoryResult.CategoryView {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val trip = tripRepository.findById(command.tripId)
            .orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val category = categoryRepository.save(
            Category(
                trip = trip,
                name = command.name,
                day = command.day,
                order = command.order,
            )
        )
        val result = CategoryResult.CategoryView.from(category)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.CATEGORY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                category = CategoryEvent(action = CategoryAction.CREATED, category = result),
            ),
        )
        return result
    }

    @Transactional(readOnly = true)
    fun getCategory(tripId: Long, categoryId: Long): CategoryResult.CategoryView {
        tripAuthorizationPolicy.isTripMember(tripId)
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }
        if (category.trip.id != tripId) throw BusinessException(ErrorCode.CATEGORY_NOT_FOUND)
        return CategoryResult.CategoryView.from(category)
    }

    @Transactional(readOnly = true)
    fun getAllCategories(tripId: Long, day: Int?): List<CategoryResult.CategoryView> {
        tripAuthorizationPolicy.isTripMember(tripId)
        val categories = if (day != null) {
            categoryRepository.findAllByTripIdAndDayOrderByOrderAsc(tripId, day)
        } else {
            categoryRepository.findAllByTripIdOrderByDayAscOrderAsc(tripId)
        }
        return categories.map(CategoryResult.CategoryView::from)
    }

    fun updateCategory(command: CategoryCommand.Update): CategoryResult.CategoryView {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val category = categoryRepository.findById(command.categoryId)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }
        if (category.trip.id != command.tripId) throw BusinessException(ErrorCode.CATEGORY_NOT_FOUND)
        command.name?.let { category.name = it }
        command.day?.let { category.day = it }
        command.order?.let { category.order = it }
        command.memo?.let { category.memo = it }
        category.updatedAt = java.time.LocalDateTime.now()
        val result = CategoryResult.CategoryView.from(category)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.CATEGORY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                category = CategoryEvent(action = CategoryAction.UPDATED, category = result),
            ),
        )
        return result
    }

    fun deleteCategory(tripId: Long, categoryId: Long) {
        tripAuthorizationPolicy.isTripMember(tripId)
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }
        if (category.trip.id != tripId) throw BusinessException(ErrorCode.CATEGORY_NOT_FOUND)
        categoryRepository.delete(category)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.CATEGORY,
                tripId = tripId,
                actorId = currentActor.requireUserId(),
                category = CategoryEvent(action = CategoryAction.DELETED, deletedCategoryId = categoryId),
            ),
        )
    }

    fun orderUpdateCategory(command: CategoryCommand.OrderUpdate): List<CategoryResult.CategoryView> {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val requestItems = command.items
        val newOrderMap = requestItems.associateBy({ it.categoryId }, { it.order })
        if (newOrderMap.size != requestItems.size) throw BusinessException(ErrorCode.DUPLICATE_CATEGORY_ID_REQUEST)
        if (newOrderMap.values.toSet().size != newOrderMap.size) throw BusinessException(ErrorCode.DUPLICATE_ORDER_REQUEST)

        val categories = categoryRepository.findAllByTripIdAndIdIn(command.tripId, command.items.map { it.categoryId })
        if (categories.size != newOrderMap.size) throw BusinessException(ErrorCode.CATEGORY_NOT_FOUND)

        val firstDay = categories.firstOrNull()?.day
        if (firstDay != null && categories.any { it.day != firstDay }) throw BusinessException(ErrorCode.CATEGORY_DAY_MISMATCH)

        categories.forEach { category ->
            category.updateOrder(newOrderMap.getValue(category.id))
            category.updatedAt = java.time.LocalDateTime.now()
        }
        val result = categories.sortedBy { it.order }.map(CategoryResult.CategoryView::from)
        tripRealtimeEventPublisher.publish(
            TripRealtimeEvent(
                type = TripRealtimeEventType.CATEGORY,
                tripId = command.tripId,
                actorId = currentActor.requireUserId(),
                category = CategoryEvent(action = CategoryAction.REORDERED, categories = result),
            ),
        )
        return result
    }
}
