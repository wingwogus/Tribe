package com.tribe.application.itinerary.place

interface PlaceSearchGateway {
    data class SearchHit(
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val primaryType: String? = null,
        val types: List<String> = emptyList(),
    )

    data class DetailsPayload(
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val primaryType: String? = null,
        val types: List<String> = emptyList(),
        val businessStatus: String? = null,
        val utcOffsetMinutes: Int? = null,
        val formattedPhoneNumber: String? = null,
        val internationalPhoneNumber: String? = null,
        val websiteUri: String? = null,
        val googleMapsUri: String? = null,
        val rating: Double? = null,
        val userRatingCount: Int? = null,
        val priceLevel: Int? = null,
        val regularOpeningHoursJson: String? = null,
        val currentOpeningHoursJson: String? = null,
        val primaryPhotoName: String? = null,
        val editorialSummary: String? = null,
        val regularOpeningPeriods: List<RegularOpeningPeriodInput> = emptyList(),
    )

    data class RegularOpeningPeriodInput(
        val dayOfWeek: Int,
        val openMinute: Int,
        val closeMinute: Int,
        val isOvernight: Boolean,
        val sequenceNo: Int,
    )

    data class NearbySearchRequest(
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Int,
        val maxResultCount: Int,
        val category: NearbyPlaceCategory,
        val language: String,
        val region: String?,
    )

    fun search(query: String?, language: String, context: PlaceSearchContext): List<SearchHit>
    fun searchNearby(request: NearbySearchRequest): List<SearchHit>
    fun getPlaceDetails(externalPlaceId: String, language: String): DetailsPayload?
    fun getPhoto(photoName: String, maxWidthPx: Int = 320): PlacePhotoMedia?
    fun directions(originPlaceId: String, destinationPlaceId: String, travelMode: String): RouteDetails?
}

data class PlacePhotoHint(
    val name: String?,
    val photoUri: String? = null,
)

data class PlacePhotoMedia(
    val bytes: ByteArray? = null,
    val contentType: String? = null,
    val redirectUri: String? = null,
)

data class PlaceSearchContext(
    val regionCode: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int? = null,
    val regionContextKey: String? = null,
)

data class RouteDetails(
    val travelMode: String,
    val originPlace: PlaceSearchGateway.SearchHit,
    val destinationPlace: PlaceSearchGateway.SearchHit,
    val totalDuration: String,
    val totalDistance: String,
    val steps: List<RouteStep>,
) {
    data class RouteStep(
        val travelMode: String,
        val instructions: String,
        val duration: String,
        val distance: String,
        val transitDetails: TransitDetails?,
    )

    data class TransitDetails(
        val lineName: String,
        val vehicleType: String,
        val vehicleIconUrl: String?,
        val numStops: Int,
        val departureStop: String,
        val arrivalStop: String,
    )
}
