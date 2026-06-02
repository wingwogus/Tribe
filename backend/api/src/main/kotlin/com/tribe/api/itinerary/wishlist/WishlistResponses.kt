package com.tribe.api.itinerary.wishlist

import com.tribe.api.itinerary.place.PlaceResponses
import com.tribe.application.itinerary.wishlist.WishlistResult
import java.math.BigDecimal

/**
 * 위시리스트 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object WishlistResponses {
    data class AdderResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val avatar: String?,
    ) {
        companion object {
            fun from(adder: WishlistResult.Adder) = AdderResponse(
                tripMemberId = adder.tripMemberId,
                memberId = adder.memberId,
                nickname = adder.nickname,
                avatar = adder.avatar,
            )
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
}
