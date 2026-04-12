package com.tribe.application.itinerary.wishlist

import com.tribe.domain.itinerary.wishlist.WishlistItem
import java.math.BigDecimal

object WishlistResult {
    data class Adder(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
    )

    data class Item(
        val wishlistItemId: Long,
        val placeId: Long,
        val name: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
        val adder: Adder,
    ) {
        companion object {
            fun from(entity: WishlistItem): Item {
                return Item(
                    wishlistItemId = entity.id,
                    placeId = entity.place.id,
                    name = entity.place.name,
                    address = entity.place.address,
                    latitude = entity.place.latitude,
                    longitude = entity.place.longitude,
                    adder = Adder(
                        tripMemberId = entity.adder.id,
                        memberId = entity.adder.member?.id,
                        nickname = entity.adder.name,
                    ),
                )
            }
        }
    }

    data class SearchPage(
        val content: List<Item>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalPages: Int,
        val totalElements: Long,
        val isLast: Boolean,
    )
}
