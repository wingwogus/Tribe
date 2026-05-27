package com.tribe.application.itinerary.place

object PlaceResult {
    data class PhotoHint(
        val name: String?,
        val photoUri: String? = null,
    )

    data class SearchItem(
        val placeId: Long? = null,
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummary? = null,
        val normalizedCategoryKey: NormalizedPlaceCategoryKey? = null,
        val photoHint: PhotoHint? = null,
        val placeDetailSummary: PlaceDetailSummary? = null,
        val openingSummary: OpeningSummary? = null,
    )

    data class Detail(
        val placeId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummary?,
        val normalizedCategoryKey: NormalizedPlaceCategoryKey?,
        val photoHint: PhotoHint?,
        val placeDetailSummary: PlaceDetailSummary?,
        val formattedPhoneNumber: String?,
        val internationalPhoneNumber: String?,
        val websiteUri: String?,
        val googleMapsUri: String?,
        val regularOpeningHoursJson: String?,
        val currentOpeningHoursJson: String?,
    )
}

data class PlaceTypeSummary(
    val primaryType: String?,
    val types: List<String>,
    val displayPrimaryLabel: String?,
)

data class PlaceDetailSummary(
    val businessStatus: String?,
    val rating: Double?,
    val userRatingCount: Int?,
    val editorialSummary: String?,
)

enum class NormalizedPlaceCategoryKey {
    KOREAN_FOOD,
    JAPANESE_FOOD,
    CHINESE_FOOD,
    RESTAURANT,
    CAFE,
    BAKERY,
    BAR,
    ATTRACTION,
    SHOPPING,
    STAY,
    PARK,
    MUSEUM,
    TRANSPORT,
    ETC,
}
