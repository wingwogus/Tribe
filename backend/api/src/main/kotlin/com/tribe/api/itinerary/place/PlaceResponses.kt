package com.tribe.api.itinerary.place

import com.fasterxml.jackson.annotation.JsonInclude
import com.tribe.application.itinerary.place.OpeningSummary
import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceResult
import com.tribe.application.itinerary.place.PlaceTypeSummary

/**
 * 장소 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object PlaceResponses {
    /**
     * Google type 요약 응답.
     */
    data class PlaceTypeSummaryResponse(
        val primaryType: String?,
        val types: List<String>,
        val displayPrimaryLabel: String?,
    ) {
        companion object {
            fun from(summary: PlaceTypeSummary) = PlaceTypeSummaryResponse(
                primaryType = summary.primaryType,
                types = summary.types,
                displayPrimaryLabel = summary.displayPrimaryLabel,
            )
        }
    }

    /**
     * 장소 사진 참조 응답.
     */
    data class PhotoHintResponse(
        val name: String?,
        val photoUri: String? = null,
    ) {
        companion object {
            fun from(hint: PlaceResult.PhotoHint) = PhotoHintResponse(
                name = hint.name,
                photoUri = hint.photoUri,
            )
        }
    }

    /**
     * 목록에서 쓰는 얇은 상세 요약.
     */
    data class PlaceDetailSummaryResponse(
        val businessStatus: String?,
        val rating: Double?,
        val userRatingCount: Int?,
        val editorialSummary: String?,
    ) {
        companion object {
            fun from(summary: PlaceDetailSummary) = PlaceDetailSummaryResponse(
                businessStatus = summary.businessStatus,
                rating = summary.rating,
                userRatingCount = summary.userRatingCount,
                editorialSummary = summary.editorialSummary,
            )
        }
    }

    /**
     * 목록에서 쓰는 영업 상태 요약.
     */
    data class OpeningSummaryResponse(
        val openNow: Boolean?,
        val nextOpenTime: String?,
        val nextCloseTime: String?,
        val source: String,
        val timezoneOffsetMinutes: Int?,
        val syncedAt: java.time.LocalDateTime?,
        val stale: Boolean,
    ) {
        companion object {
            fun from(summary: OpeningSummary) = OpeningSummaryResponse(
                openNow = summary.openNow,
                nextOpenTime = summary.nextOpenTime,
                nextCloseTime = summary.nextCloseTime,
                source = summary.source.name,
                timezoneOffsetMinutes = summary.timezoneOffsetMinutes,
                syncedAt = summary.syncedAt,
                stale = summary.stale,
            )
        }
    }

    /**
     * 검색/주변/resolve 공통 목록 응답.
     */
    data class SearchResponse(
        val placeId: Long? = null,
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummaryResponse? = null,
        val normalizedCategoryKey: String? = null,
        @field:JsonInclude(JsonInclude.Include.NON_NULL)
        val photoHint: PhotoHintResponse? = null,
        @field:JsonInclude(JsonInclude.Include.NON_NULL)
        val placeDetailSummary: PlaceDetailSummaryResponse? = null,
        val openingSummary: OpeningSummaryResponse? = null,
    ) {
        companion object {
            fun from(result: PlaceResult.SearchItem) = SearchResponse(
                placeId = result.placeId,
                externalPlaceId = result.externalPlaceId,
                placeName = result.placeName,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                placeTypeSummary = result.placeTypeSummary?.let(PlaceTypeSummaryResponse::from),
                normalizedCategoryKey = result.normalizedCategoryKey?.name,
                photoHint = result.photoHint?.let(PhotoHintResponse::from),
                placeDetailSummary = result.placeDetailSummary?.let(PlaceDetailSummaryResponse::from),
                openingSummary = result.openingSummary?.let(OpeningSummaryResponse::from),
            )

            // 주변 검색 목록은 빠른 렌더링을 위해 사진/상세 요약 제거.
            fun fromNearby(result: PlaceResult.SearchItem) = from(
                result.copy(
                    photoHint = null,
                    placeDetailSummary = null,
                ),
            )
        }
    }

    /**
     * 내부 Place 상세 응답.
     */
    data class DetailResponse(
        val placeId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummaryResponse?,
        val normalizedCategoryKey: String?,
        val photoHint: PhotoHintResponse?,
        val placeDetailSummary: PlaceDetailSummaryResponse?,
        val formattedPhoneNumber: String?,
        val internationalPhoneNumber: String?,
        val websiteUri: String?,
        val googleMapsUri: String?,
        val priceLevel: Int?,
        val regularOpeningHoursJson: String?,
        val currentOpeningHoursJson: String?,
    ) {
        companion object {
            fun from(view: PlaceResult.Detail) = DetailResponse(
                placeId = view.placeId,
                externalPlaceId = view.externalPlaceId,
                placeName = view.placeName,
                address = view.address,
                latitude = view.latitude,
                longitude = view.longitude,
                placeTypeSummary = view.placeTypeSummary?.let(PlaceTypeSummaryResponse::from),
                normalizedCategoryKey = view.normalizedCategoryKey?.name,
                photoHint = view.photoHint?.let(PhotoHintResponse::from),
                placeDetailSummary = view.placeDetailSummary?.let(PlaceDetailSummaryResponse::from),
                formattedPhoneNumber = view.formattedPhoneNumber,
                internationalPhoneNumber = view.internationalPhoneNumber,
                websiteUri = view.websiteUri,
                googleMapsUri = view.googleMapsUri,
                priceLevel = view.priceLevel,
                regularOpeningHoursJson = view.regularOpeningHoursJson,
                currentOpeningHoursJson = view.currentOpeningHoursJson,
            )
        }
    }
}
