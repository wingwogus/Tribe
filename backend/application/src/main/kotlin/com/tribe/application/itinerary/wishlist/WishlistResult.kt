package com.tribe.application.itinerary.wishlist

import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.application.itinerary.place.OpeningSummary
import com.tribe.application.itinerary.place.OpeningSummaryAssembler
import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.itinerary.place.PlaceTypeSummary
import com.tribe.domain.itinerary.wishlist.WishlistItem
import java.math.BigDecimal

object WishlistResult {
    private val openingSummaryAssembler = OpeningSummaryAssembler()

    data class PhotoHint(
        val name: String?,
        val photoUri: String?,
    )

    data class Adder(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
    )

    data class Item(
        val wishlistItemId: Long,
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
        val adder: Adder,
        val likeCount: Long = 0L,
        val likedByMe: Boolean = false,
    ) {
        companion object {
            fun from(
                entity: WishlistItem,
                likeCount: Long = 0L,
                likedByMe: Boolean = false,
            ): Item {
                val assembler = PlaceResultAssembler()
                val photoHint = assembler.toPhotoHint(entity.place)
                val placeTypeSummary = assembler.toPlaceTypeSummary(entity.place)
                return Item(
                    wishlistItemId = entity.id,
                    placeId = entity.place.id,
                    externalPlaceId = entity.place.externalPlaceId,
                    name = entity.place.name,
                    address = entity.place.address,
                    latitude = entity.place.latitude,
                    longitude = entity.place.longitude,
                    placeTypeSummary = placeTypeSummary,
                    normalizedCategoryKey = PlaceResultAssembler.toNormalizedCategoryKey(placeTypeSummary),
                    photoHint = photoHint?.let { PhotoHint(it.name, it.photoUri) },
                    placeDetailSummary = assembler.toDetailSummary(entity.place),
                    openingSummary = openingSummaryAssembler.toOpeningSummary(entity.place),
                    adder = Adder(
                        tripMemberId = entity.adder.id,
                        memberId = entity.adder.member?.id,
                        nickname = entity.adder.name,
                    ),
                    likeCount = likeCount,
                    likedByMe = likedByMe,
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

    data class LikeSummary(
        val likeCount: Long,
        val likedByMe: Boolean,
    )
}
