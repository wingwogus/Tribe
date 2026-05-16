package com.tribe.api.itinerary.wishlist

import com.tribe.api.itinerary.place.PlaceResponses
import com.tribe.application.itinerary.wishlist.WishlistResult
import java.math.BigDecimal

object WishlistResponses {
    data class AdderResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
    ) {
        companion object {
            fun from(adder: WishlistResult.Adder) = AdderResponse(adder.tripMemberId, adder.memberId, adder.nickname)
        }
    }

    data class WishlistItemResponse(
        val wishlistItemId: Long,
        val placeId: Long,
        val name: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
        val placeTypeSummary: PlaceResponses.PlaceTypeSummaryResponse?,
        val normalizedCategoryKey: String?,
        val photoHint: PlaceResponses.PhotoHintResponse?,
        val placeDetailSummary: PlaceResponses.PlaceDetailSummaryResponse?,
        val adder: AdderResponse,
        val likeCount: Long,
        val likedByMe: Boolean,
    ) {
        companion object {
            fun from(item: WishlistResult.Item) = WishlistItemResponse(
                item.wishlistItemId,
                item.placeId,
                item.name,
                item.address,
                item.latitude,
                item.longitude,
                item.placeTypeSummary?.let(PlaceResponses.PlaceTypeSummaryResponse::from),
                item.normalizedCategoryKey?.name,
                item.photoHint?.let { PlaceResponses.PhotoHintResponse(it.name, it.photoUri) },
                item.placeDetailSummary?.let(PlaceResponses.PlaceDetailSummaryResponse::from),
                AdderResponse.from(item.adder),
                item.likeCount,
                item.likedByMe,
            )
        }
    }

    data class WishlistSearchResponse(
        val content: List<WishlistItemResponse>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalPages: Int,
        val totalElements: Long,
        val isLast: Boolean,
    ) {
        companion object {
            fun from(page: WishlistResult.SearchPage) = WishlistSearchResponse(
                content = page.content.map(WishlistItemResponse::from),
                pageNumber = page.pageNumber,
                pageSize = page.pageSize,
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                isLast = page.isLast,
            )
        }
    }

    data class WishlistLikeResponse(
        val likeCount: Long,
        val likedByMe: Boolean,
    ) {
        companion object {
            fun from(summary: WishlistResult.LikeSummary) = WishlistLikeResponse(
                likeCount = summary.likeCount,
                likedByMe = summary.likedByMe,
            )
        }
    }
}
