package com.tribe.application.itinerary.place

/**
 * 장소 검색 외부 port.
 *
 * Google Places 같은 외부 공급자를 application use case에서 분리하는 계약.
 */
interface PlaceSearchGateway {
    /**
     * 외부 검색 후보.
     *
     * 아직 내부 `Place` 저장이 확정되지 않은 목록 표시 단위.
     */
    data class SearchHit(
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val primaryType: String? = null,
        val types: List<String> = emptyList(),
    )

    /**
     * 상세 동기화 payload.
     *
     * 내부 Place 본문과 detail snapshot으로 나누어 저장할 Google 상세 정보.
     */
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

    /**
     * Google regularOpeningHours period 저장 입력.
     */
    data class RegularOpeningPeriodInput(
        val dayOfWeek: Int,
        val openMinute: Int,
        val closeMinute: Int,
        val isOvernight: Boolean,
        val sequenceNo: Int,
    )

    /**
     * 지도 중심 주변 검색 요청.
     *
     * application 검증/정규화가 끝난 값만 gateway로 전달.
     */
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
    fun getPlaceSummary(externalPlaceId: String, language: String): SearchHit?
    fun getPlaceDetails(externalPlaceId: String, language: String): DetailsPayload?
    fun getPhoto(photoName: String, maxWidthPx: Int = 320): PlacePhotoMedia?
    fun directions(originPlaceId: String, destinationPlaceId: String, travelMode: String): RouteDetails?
}

/**
 * 장소 사진 힌트.
 *
 * 목록/상세 응답에서 사진 참조를 느슨하게 전달하는 shape.
 */
data class PlacePhotoHint(
    val name: String?,
    val photoUri: String? = null,
)

/**
 * 장소 사진 media 응답.
 *
 * binary 응답과 redirect URI 응답을 같은 port로 표현.
 */
data class PlacePhotoMedia(
    val bytes: ByteArray? = null,
    val contentType: String? = null,
    val redirectUri: String? = null,
)

/**
 * 텍스트 검색 문맥.
 *
 * 지역 코드와 지도 중심 bias를 cache key와 외부 요청에 함께 전달.
 */
data class PlaceSearchContext(
    val regionCode: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int? = null,
    val regionContextKey: String? = null,
)

/**
 * 경로 요약.
 *
 * Directions 원본 응답을 프론트가 바로 표시 가능한 단계 목록으로 축소.
 */
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
