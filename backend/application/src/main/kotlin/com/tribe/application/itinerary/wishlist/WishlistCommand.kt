package com.tribe.application.itinerary.wishlist

import java.math.BigDecimal

/**
 * 위시리스트 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object WishlistCommand {
    data class Add(
        val tripId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
    )

    data class AddFromMemberWishlist(
        val tripId: Long,
        val memberWishlistItemId: Long,
    )

    data class AddFromPlace(
        val tripId: Long,
        val placeId: Long,
    )

    data class Delete(
        val tripId: Long,
        val wishlistItemIds: List<Long>,
    )
}
