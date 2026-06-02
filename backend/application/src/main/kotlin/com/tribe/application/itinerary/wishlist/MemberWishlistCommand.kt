package com.tribe.application.itinerary.wishlist

import java.math.BigDecimal

/**
 * 위시리스트 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object MemberWishlistCommand {
    data class Add(
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
    )

    data class Delete(
        val memberWishlistItemIds: List<Long>,
    )
}
