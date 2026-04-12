package com.tribe.application.itinerary.item

import com.tribe.domain.itinerary.item.ItineraryItem
import java.time.LocalDateTime

object ItemResult {
    data class LocationInfo(
        val lat: Double,
        val lng: Double,
        val address: String?,
    )

    data class ItemView(
        val itemId: Long,
        val categoryId: Long,
        val categoryName: String,
        val tripId: Long,
        val day: Int,
        val placeId: Long?,
        val name: String,
        val title: String?,
        val time: LocalDateTime?,
        val order: Int,
        val memo: String?,
        val location: LocationInfo?,
    ) {
        companion object {
            fun from(item: ItineraryItem) = ItemView(
                itemId = item.id,
                categoryId = item.category.id,
                categoryName = item.category.name,
                tripId = item.category.trip.id,
                day = item.category.day,
                placeId = item.place?.id,
                name = item.place?.name ?: item.title ?: "",
                title = item.title,
                time = item.time,
                order = item.order,
                memo = item.memo,
                location = item.place?.let {
                    LocationInfo(
                        lat = it.latitude.toDouble(),
                        lng = it.longitude.toDouble(),
                        address = it.address,
                    )
                },
            )
        }
    }
}
