package com.tribe.application.itinerary.place

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.domain.itinerary.place.Place
import org.springframework.stereotype.Component

@Component
class PlaceResultAssembler {
    fun toNormalizedCategoryKey(place: Place?): NormalizedPlaceCategoryKey? =
        Companion.toNormalizedCategoryKey(toPlaceTypeSummary(place))

    fun toPlaceTypeSummary(place: Place?): PlaceTypeSummary? {
        if (place == null) return null
        return fromGoogleTypesJson(
            primaryType = place.googlePrimaryType,
            googleTypesJson = place.googleTypesJson,
        )
    }

    fun toPhotoHint(place: Place?): PlaceResult.PhotoHint? {
        val photoName = place?.detailSnapshot?.primaryPhotoName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return PlaceResult.PhotoHint(name = photoName, photoUri = null)
    }

    fun toDetailSummary(place: Place?): PlaceDetailSummary? {
        val snapshot = place?.detailSnapshot ?: return null
        return PlaceDetailSummary(
            businessStatus = place.businessStatus,
            rating = snapshot.rating,
            userRatingCount = snapshot.userRatingCount,
            editorialSummary = snapshot.editorialSummary,
        )
    }

    fun toSearchItem(
        hit: PlaceSearchGateway.SearchHit,
        canonicalPlace: Place?,
    ): PlaceResult.SearchItem {
        val placeTypeSummary = fromRawTypes(hit.primaryType, hit.types)
            ?: toPlaceTypeSummary(canonicalPlace)
        return PlaceResult.SearchItem(
            placeId = canonicalPlace?.id,
            externalPlaceId = hit.externalPlaceId,
            placeName = hit.placeName,
            address = hit.address,
            latitude = hit.latitude,
            longitude = hit.longitude,
            placeTypeSummary = placeTypeSummary,
            normalizedCategoryKey = Companion.toNormalizedCategoryKey(placeTypeSummary)
                ?: toNormalizedCategoryKey(canonicalPlace),
            photoHint = toPhotoHint(canonicalPlace),
            placeDetailSummary = toDetailSummary(canonicalPlace),
        )
    }

    fun toDetail(place: Place): PlaceResult.Detail {
        val placeTypeSummary = toPlaceTypeSummary(place)
        return PlaceResult.Detail(
            placeId = place.id,
            externalPlaceId = place.externalPlaceId,
            placeName = place.name,
            address = place.address,
            latitude = place.latitude.toDouble(),
            longitude = place.longitude.toDouble(),
            placeTypeSummary = placeTypeSummary,
            normalizedCategoryKey = Companion.toNormalizedCategoryKey(placeTypeSummary),
            photoHint = toPhotoHint(place),
            placeDetailSummary = toDetailSummary(place),
            formattedPhoneNumber = place.detailSnapshot?.formattedPhoneNumber,
            internationalPhoneNumber = place.detailSnapshot?.internationalPhoneNumber,
            websiteUri = place.detailSnapshot?.websiteUri,
            googleMapsUri = place.detailSnapshot?.googleMapsUri,
            regularOpeningHoursJson = place.detailSnapshot?.regularOpeningHoursJson,
            currentOpeningHoursJson = place.detailSnapshot?.currentOpeningHoursJson,
        )
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()

        fun fromRawTypes(primaryType: String?, types: List<String>): PlaceTypeSummary? {
            if (primaryType == null && types.isEmpty()) {
                return null
            }

            return PlaceTypeSummary(
                primaryType = primaryType,
                types = types,
                displayPrimaryLabel = toDisplayPrimaryLabel(primaryType),
            )
        }

        fun fromGoogleTypesJson(primaryType: String?, googleTypesJson: String?): PlaceTypeSummary? =
            fromRawTypes(primaryType, decodeGoogleTypes(googleTypesJson))

        fun toNormalizedCategoryKey(placeTypeSummary: PlaceTypeSummary?): NormalizedPlaceCategoryKey? =
            placeTypeSummary?.let { normalizeCategory(it.primaryType, it.types) }

        fun decodeGoogleTypes(json: String?): List<String> =
            json?.let {
                runCatching { objectMapper.readValue(it, Array<String>::class.java).toList() }.getOrDefault(emptyList())
            } ?: emptyList()

        fun toDisplayPrimaryLabel(primaryType: String?): String? =
            primaryType?.replace('_', ' ')

        fun normalizeCategory(
            primaryType: String?,
            types: List<String>,
        ): NormalizedPlaceCategoryKey? {
            val candidates = buildList {
                primaryType?.let(::add)
                addAll(types)
            }.map { it.lowercase() }

            if (candidates.isEmpty()) {
                return null
            }

            return when {
                candidates.any { it in setOf("korean_restaurant") } -> NormalizedPlaceCategoryKey.KOREAN_FOOD
                candidates.any { it in setOf("japanese_restaurant", "ramen_restaurant", "sushi_restaurant") } -> NormalizedPlaceCategoryKey.JAPANESE_FOOD
                candidates.any { it in setOf("chinese_restaurant") } -> NormalizedPlaceCategoryKey.CHINESE_FOOD
                candidates.any { it in setOf("cafe", "coffee_shop", "tea_house") } -> NormalizedPlaceCategoryKey.CAFE
                candidates.any { it in setOf("bakery") } -> NormalizedPlaceCategoryKey.BAKERY
                candidates.any { it in setOf("bar", "pub", "night_club") } -> NormalizedPlaceCategoryKey.BAR
                candidates.any { it in setOf("tourist_attraction", "historical_place", "monument", "visitor_center", "amusement_park", "aquarium", "zoo") } -> NormalizedPlaceCategoryKey.ATTRACTION
                candidates.any { it in setOf("shopping_mall", "department_store", "store", "market", "clothing_store") } -> NormalizedPlaceCategoryKey.SHOPPING
                candidates.any { it in setOf("lodging", "hotel", "motel", "resort_hotel", "hostel") } -> NormalizedPlaceCategoryKey.STAY
                candidates.any { it in setOf("park", "national_park") } -> NormalizedPlaceCategoryKey.PARK
                candidates.any { it in setOf("museum", "art_gallery") } -> NormalizedPlaceCategoryKey.MUSEUM
                candidates.any { it in setOf("subway_station", "train_station", "airport", "bus_station", "transit_station") } -> NormalizedPlaceCategoryKey.TRANSPORT
                candidates.any { it in setOf("restaurant", "meal_takeaway", "meal_delivery", "food_court") } -> NormalizedPlaceCategoryKey.RESTAURANT
                else -> NormalizedPlaceCategoryKey.ETC
            }
        }
    }
}
