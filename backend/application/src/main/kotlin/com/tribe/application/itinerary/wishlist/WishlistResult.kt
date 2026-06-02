package com.tribe.application.itinerary.wishlist

import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.itinerary.place.PlaceTypeSummary
import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.domain.itinerary.wishlist.WishlistItem
import java.math.BigDecimal

/**
 * 위시리스트 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object WishlistResult {
    data class PhotoHint(
        val name: String?,
        val photoUri: String?,
    )

    data class Adder(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val avatar: String?,
    )

    data class Item(
        val wishlistItemId: Long,
        val placeId: Long,
        val name: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
        val placeTypeSummary: PlaceTypeSummary?,
        val normalizedCategoryKey: NormalizedPlaceCategoryKey?,
        val photoHint: PhotoHint?,
        val placeDetailSummary: PlaceDetailSummary?,
        val adder: Adder,
    ) {
        companion object {
            fun from(entity: WishlistItem): Item {
                val assembler = PlaceResultAssembler()
                val placeTypeSummary = assembler.toPlaceTypeSummary(entity.place)
                return Item(
                    wishlistItemId = entity.id,
                    placeId = entity.place.id,
                    name = entity.place.name,
                    address = entity.place.address,
                    latitude = entity.place.latitude,
                    longitude = entity.place.longitude,
                    placeTypeSummary = placeTypeSummary,
                    normalizedCategoryKey = PlaceResultAssembler.toNormalizedCategoryKey(placeTypeSummary),
                    photoHint = null,
                    placeDetailSummary = assembler.toDetailSummary(entity.place),
                    adder = Adder(
                        tripMemberId = entity.adder.id,
                        memberId = entity.adder.member?.id,
                        nickname = entity.adder.name,
                        avatar = entity.adder.member?.avatar,
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
