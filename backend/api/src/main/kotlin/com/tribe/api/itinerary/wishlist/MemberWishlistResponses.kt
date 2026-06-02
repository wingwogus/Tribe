package com.tribe.api.itinerary.wishlist

import com.tribe.api.itinerary.place.PlaceResponses
import com.tribe.application.itinerary.wishlist.MemberWishlistResult
import java.math.BigDecimal

/**
 * 위시리스트 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object MemberWishlistResponses {
    data class MemberWishlistItemResponse(
        val memberWishlistItemId: Long,
        val placeId: Long,
        val externalPlaceId: String,
        val name: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
        val placeTypeSummary: PlaceResponses.PlaceTypeSummaryResponse?,
        val normalizedCategoryKey: String?,
        val photoHint: PlaceResponses.PhotoHintResponse?,
        val placeDetailSummary: PlaceResponses.PlaceDetailSummaryResponse?,
        val openingSummary: PlaceResponses.OpeningSummaryResponse?,
    ) {
        companion object {
            fun from(item: MemberWishlistResult.Item) = MemberWishlistItemResponse(
                memberWishlistItemId = item.memberWishlistItemId,
                placeId = item.placeId,
                externalPlaceId = item.externalPlaceId,
                name = item.name,
                address = item.address,
                latitude = item.latitude,
                longitude = item.longitude,
                placeTypeSummary = item.placeTypeSummary?.let(PlaceResponses.PlaceTypeSummaryResponse::from),
                normalizedCategoryKey = item.normalizedCategoryKey?.name,
                photoHint = item.photoHint?.let { PlaceResponses.PhotoHintResponse(it.name, it.photoUri) },
                placeDetailSummary = item.placeDetailSummary?.let(PlaceResponses.PlaceDetailSummaryResponse::from),
                openingSummary = item.openingSummary?.let(PlaceResponses.OpeningSummaryResponse::from),
            )
        }
    }

    data class MemberWishlistSearchResponse(
        val content: List<MemberWishlistItemResponse>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalPages: Int,
        val totalElements: Long,
        val isLast: Boolean,
    ) {
        companion object {
            fun from(page: MemberWishlistResult.SearchPage) = MemberWishlistSearchResponse(
                content = page.content.map(MemberWishlistItemResponse::from),
                pageNumber = page.pageNumber,
                pageSize = page.pageSize,
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                isLast = page.isLast,
            )
        }
    }
}
