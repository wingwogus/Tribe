package com.tribe.application.itinerary.wishlist

import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.application.itinerary.place.OpeningSummary
import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceTypeSummary
import java.math.BigDecimal

/**
 * 위시리스트 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object MemberWishlistResult {
    data class PhotoHint(
        val name: String?,
        val photoUri: String?,
    )

    data class Item(
        val memberWishlistItemId: Long,
        val placeId: Long,
        val externalPlaceId: String,
        val name: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
        val placeTypeSummary: PlaceTypeSummary?,
        val normalizedCategoryKey: NormalizedPlaceCategoryKey?,
        val photoHint: PhotoHint?,
        val placeDetailSummary: PlaceDetailSummary?,
        val openingSummary: OpeningSummary? = null,
    )

    data class SearchPage(
        val content: List<Item>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalPages: Int,
        val totalElements: Long,
        val isLast: Boolean,
    )
}
