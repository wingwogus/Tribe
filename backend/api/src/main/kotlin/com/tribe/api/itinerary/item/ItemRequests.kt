package com.tribe.api.itinerary.item

import java.time.LocalDateTime

object ItemRequests {
    data class CreateRequest(
        val visitDay: Int,
        val placeId: Long? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class UpdateRequest(
        val visitDay: Int? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class OrderUpdateRequest(
        val items: List<OrderItemRequest>,
    )

    data class OrderItemRequest(
        val itemId: Long,
        val visitDay: Int,
        val itemOrder: Int,
    )
}
