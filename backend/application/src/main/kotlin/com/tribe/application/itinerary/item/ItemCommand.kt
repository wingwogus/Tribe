package com.tribe.application.itinerary.item

import java.time.LocalDateTime

object ItemCommand {
    data class Create(
        val tripId: Long,
        val visitDay: Int,
        val placeId: Long? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class Update(
        val tripId: Long,
        val itemId: Long,
        val visitDay: Int? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class OrderUpdate(
        val tripId: Long,
        val items: List<OrderItem>,
    )

    data class OrderItem(
        val itemId: Long,
        val visitDay: Int,
        val itemOrder: Int,
    )
}
