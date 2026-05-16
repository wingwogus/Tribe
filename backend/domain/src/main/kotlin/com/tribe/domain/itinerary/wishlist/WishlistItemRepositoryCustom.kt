package com.tribe.domain.itinerary.wishlist

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface WishlistItemRepositoryCustom {
    fun findPageByTrip(
        tripId: Long,
        query: String?,
        sort: TripWishlistSort?,
        pageable: Pageable,
    ): Page<WishlistItem>
}
