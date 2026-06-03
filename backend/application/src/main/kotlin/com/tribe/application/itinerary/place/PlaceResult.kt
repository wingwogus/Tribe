package com.tribe.application.itinerary.place

/**
 * 장소 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object PlaceResult {
    /**
     * 사진 참조 result.
     */
    data class PhotoHint(
        val name: String?,
        val photoUri: String? = null,
    )

    /**
     * 검색 후보 result.
     *
     * placeId가 있으면 이미 저장된 내부 Place와 병합된 후보.
     */
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

    /**
     * 내부 Place 상세 result.
     */
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
        val priceLevel: Int?,
        val regularOpeningHoursJson: String?,
        val currentOpeningHoursJson: String?,
    )
}

/**
 * Google type 요약.
 */
data class PlaceTypeSummary(
    val primaryType: String?,
    val types: List<String>,
    val displayPrimaryLabel: String?,
)

/**
 * 목록 표시용 상세 요약.
 */
data class PlaceDetailSummary(
    val businessStatus: String?,
    val rating: Double?,
    val userRatingCount: Int?,
    val editorialSummary: String?,
)

/**
 * 앱 공통 장소 category key.
 */
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
