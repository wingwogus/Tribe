package com.tribe.api.itinerary.place

object PlaceRequests {
    data class NearbySearchRequest(
        val latitude: Double?,
        val longitude: Double?,
        val radiusMeters: Int?,
        val maxResultCount: Int?,
        val category: String?,
        val language: String?,
        val region: String?,
    )
}
