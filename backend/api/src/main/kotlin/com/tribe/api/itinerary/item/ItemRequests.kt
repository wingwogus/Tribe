package com.tribe.api.itinerary.item

import com.tribe.application.itinerary.item.ItemCommand
import java.time.LocalDateTime

object ItemRequests {
    data class CreateRequest(
        val visitDay: Int,
        val placeId: Long? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    ) {
        fun toCommand(tripId: Long): ItemCommand.Create = ItemCommand.Create(
            tripId = tripId,
            visitDay = visitDay,
            placeId = placeId,
            title = title,
            time = time,
            memo = memo,
        )
    }

    data class CreateFromMemberWishlistRequest(
        val memberWishlistItemId: Long,
        val visitDay: Int,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    ) {
        fun toCommand(tripId: Long): ItemCommand.CreateFromMemberWishlist = ItemCommand.CreateFromMemberWishlist(
            tripId = tripId,
            memberWishlistItemId = memberWishlistItemId,
            visitDay = visitDay,
            time = time,
            memo = memo,
        )
    }

    data class UpdateRequest(
        val visitDay: Int? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    ) {
        fun toCommand(tripId: Long, itemId: Long): ItemCommand.Update = ItemCommand.Update(
            tripId = tripId,
            itemId = itemId,
            visitDay = visitDay,
            title = title,
            time = time,
            memo = memo,
        )
    }

    data class OrderUpdateRequest(
        val items: List<OrderItemRequest>,
    ) {
        fun toCommand(tripId: Long): ItemCommand.OrderUpdate = ItemCommand.OrderUpdate(
            tripId = tripId,
            items = items.map(OrderItemRequest::toCommand),
        )
    }

    data class OrderItemRequest(
        val itemId: Long,
        val visitDay: Int,
        val itemOrder: Int,
    ) {
        fun toCommand(): ItemCommand.OrderItem = ItemCommand.OrderItem(
            itemId = itemId,
            visitDay = visitDay,
            itemOrder = itemOrder,
        )
    }
}
