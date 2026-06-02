package com.tribe.application.itinerary.item

import java.time.LocalDateTime

/**
 * 일정 아이템 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object ItemCommand {
    data class Create(
        val tripId: Long,
        val visitDay: Int,
        val placeId: Long? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class CreateFromMemberWishlist(
        val tripId: Long,
        val memberWishlistItemId: Long,
        val visitDay: Int,
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
