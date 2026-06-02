package com.tribe.api.itinerary.place

/**
 * 장소 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object PlaceRequests {
    /**
     * 외부 후보를 내부 Place로 확정하는 요청.
     */
    data class ResolveExternalPlaceRequest(
        val externalPlaceId: String?,
        val language: String? = "ko",
    )

    /**
     * 지도 중심 주변 장소 검색 요청.
     *
     * 좌표/반경/category는 application 계층에서 최종 검증.
     */
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
