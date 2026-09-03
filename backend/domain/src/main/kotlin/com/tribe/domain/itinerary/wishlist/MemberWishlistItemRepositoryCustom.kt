package com.tribe.domain.itinerary.wishlist

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface MemberWishlistItemRepositoryCustom {
    fun findPageByMember(
        memberId: Long,
        query: String?,
        sort: AccountWishlistSort?,
        pageable: Pageable,
    ): Page<MemberWishlistItem>
}
