package com.tribe.application.itinerary.item

import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.itinerary.place.PlaceTypeSummary
import com.tribe.domain.itinerary.item.ItineraryItem
import java.time.LocalDateTime

/**
 * 일정 아이템 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object ItemResult {
    data class PhotoHint(
        val name: String?,
        val photoUri: String? = null,
    )

    data class LocationInfo(
        val lat: Double,
        val lng: Double,
        val address: String?,
    )

    data class Item(
        val itemId: Long,
        val tripId: Long,
        val visitDay: Int,
        val itemOrder: Int,
        val placeId: Long?,
        val externalPlaceId: String?,
        val name: String,
        val title: String?,
        val time: LocalDateTime?,
        val memo: String?,
        val location: LocationInfo?,
        val placeTypeSummary: PlaceTypeSummary?,
        val normalizedCategoryKey: NormalizedPlaceCategoryKey?,
        val photoHint: PhotoHint?,
        val placeDetailSummary: PlaceDetailSummary?,
        val openingStatusWarning: String?,
    ) {
        companion object {
            fun from(
                item: ItineraryItem,
                placeTypeSummary: PlaceTypeSummary? = null,
                photoHint: PhotoHint? = null,
                placeDetailSummary: PlaceDetailSummary? = null,
                openingStatusWarning: String? = null,
            ) = Item(
                itemId = item.id,
                tripId = item.trip.id,
                visitDay = item.visitDay,
                itemOrder = item.order,
                placeId = item.place?.id,
                externalPlaceId = item.place?.externalPlaceId,
                name = item.place?.name ?: item.title ?: "",
                title = item.title,
                time = item.time,
                memo = item.memo,
                location = item.place?.let {
                    LocationInfo(
                        lat = it.latitude.toDouble(),
                        lng = it.longitude.toDouble(),
                        address = it.address,
                    )
                },
                placeTypeSummary = placeTypeSummary,
                normalizedCategoryKey = PlaceResultAssembler.toNormalizedCategoryKey(placeTypeSummary),
                photoHint = photoHint,
                placeDetailSummary = placeDetailSummary,
                openingStatusWarning = openingStatusWarning,
            )
        }
    }
}
