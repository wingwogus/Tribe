package com.tribe.application.trip.event

import com.tribe.application.itinerary.category.CategoryResult
import com.tribe.application.itinerary.item.ItemResult
import com.tribe.application.itinerary.wishlist.WishlistResult
import com.tribe.application.trip.core.TripResult
import java.time.LocalDate

enum class TripRealtimeEventType {
    TRIP_LIFECYCLE,
    TRIP_MEMBER,
    CATEGORY,
    ITINERARY,
    WISHLIST,
}

data class TripRealtimeEvent(
    val type: TripRealtimeEventType,
    val tripId: Long,
    val actorId: Long,
    val lifecycle: TripLifecycleEvent? = null,
    val member: TripMemberEvent? = null,
    val category: CategoryEvent? = null,
    val itinerary: ItineraryEvent? = null,
    val wishlist: WishlistEvent? = null,
)

enum class TripLifecycleAction {
    CREATED,
    UPDATED,
    DELETED,
    IMPORTED,
}

data class TripLifecycleEvent(
    val action: TripLifecycleAction,
    val trip: TripSummary? = null,
    val deletedTripId: Long? = null,
)

data class TripSummary(
    val tripId: Long,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val country: String,
) {
    companion object {
        fun from(trip: TripResult.TripDetail) = TripSummary(
            tripId = trip.tripId,
            title = trip.title,
            startDate = trip.startDate,
            endDate = trip.endDate,
            country = trip.country,
        )
    }
}

enum class TripMemberAction {
    GUEST_ADDED,
    GUEST_DELETED,
    MEMBER_JOINED,
    MEMBER_LEFT,
    MEMBER_KICKED,
    ROLE_CHANGED,
}

data class TripMemberEvent(
    val action: TripMemberAction,
    val member: TripResult.MemberSummary,
)

enum class CategoryAction {
    CREATED,
    UPDATED,
    DELETED,
    REORDERED,
}

data class CategoryEvent(
    val action: CategoryAction,
    val category: CategoryResult.CategoryView? = null,
    val categories: List<CategoryResult.CategoryView>? = null,
    val deletedCategoryId: Long? = null,
)

enum class ItineraryAction {
    CREATED,
    UPDATED,
    DELETED,
    REORDERED,
}

data class ItineraryEvent(
    val action: ItineraryAction,
    val item: ItemResult.ItemView? = null,
    val items: List<ItemResult.ItemView>? = null,
    val deletedItemId: Long? = null,
)

enum class WishlistAction {
    ADDED,
    DELETED,
}

data class WishlistEvent(
    val action: WishlistAction,
    val item: WishlistResult.Item? = null,
    val deletedItemIds: List<Long>? = null,
)
