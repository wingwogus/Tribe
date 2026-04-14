package com.tribe.application.itinerary.item

import com.tribe.domain.itinerary.item.ItineraryItem
import java.time.LocalDateTime

object ItemResult {
    data class PlaceTypeSummary(
        val primaryType: String?,
        val types: List<String>,
        val localizedPrimaryLabel: String?,
    )

    data class LocationInfo(
        val lat: Double,
        val lng: Double,
        val address: String?,
    )

    data class ItemView(
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
        val openingStatusWarning: String?,
    ) {
        companion object {
            fun from(item: ItineraryItem) = ItemView(
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
                placeTypeSummary = null,
                openingStatusWarning = null,
            )
        }
    }
}
