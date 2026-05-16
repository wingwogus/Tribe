package com.tribe.application.itinerary.wishlist

import java.math.BigDecimal

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
