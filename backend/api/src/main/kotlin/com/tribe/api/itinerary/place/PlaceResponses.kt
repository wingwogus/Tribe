package com.tribe.api.itinerary.place

import com.tribe.application.itinerary.place.OpeningSummary
import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceResult
import com.tribe.application.itinerary.place.PlaceTypeSummary

object PlaceResponses {
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

    data class SearchResponse(
        val placeId: Long? = null,
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummaryResponse? = null,
        val normalizedCategoryKey: String? = null,
        val photoHint: PhotoHintResponse? = null,
        val placeDetailSummary: PlaceDetailSummaryResponse? = null,
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
            )
        }
    }

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
                regularOpeningHoursJson = view.regularOpeningHoursJson,
                currentOpeningHoursJson = view.currentOpeningHoursJson,
            )
        }
    }
}
