package com.tribe.application.itinerary.place

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.domain.itinerary.place.Place
import org.springframework.stereotype.Component

/**
 * 장소 응답 assembler.
 *
 * 외부 후보와 이미 저장된 Place를 API 응답 가능한 shape로 조립.
 */
@Component
class PlaceResultAssembler {
    private val openingSummaryAssembler = OpeningSummaryAssembler()

    fun toNormalizedCategoryKey(place: Place?): NormalizedPlaceCategoryKey? =
        Companion.toNormalizedCategoryKey(toPlaceTypeSummary(place))

    fun toPlaceTypeSummary(place: Place?): PlaceTypeSummary? {
        // 저장된 Place의 Google type JSON을 읽어 표시 라벨과 normalized category 근거 생성.
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
        // 목록 응답에는 상세 전체 대신 평점/상태/요약만 얇게 포함.
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
        savedPlace: Place?,
    ): PlaceResult.SearchItem {
        // 외부 후보 type을 우선 사용하고, 저장된 Place type은 보조 근거로 사용.
        val placeTypeSummary = fromRawTypes(hit.primaryType, hit.types)
            ?: toPlaceTypeSummary(savedPlace)
        return PlaceResult.SearchItem(
            placeId = savedPlace?.id,
            externalPlaceId = hit.externalPlaceId,
            placeName = hit.placeName,
            address = hit.address,
            latitude = hit.latitude,
            longitude = hit.longitude,
            placeTypeSummary = placeTypeSummary,
            normalizedCategoryKey = Companion.toNormalizedCategoryKey(placeTypeSummary)
                ?: toNormalizedCategoryKey(savedPlace),
            photoHint = toPhotoHint(savedPlace),
            placeDetailSummary = toDetailSummary(savedPlace) ?: hit.toDetailSummary(),
            openingSummary = savedPlace?.let(openingSummaryAssembler::toOpeningSummary) ?: hit.openingSummary,
        )
    }

    fun toNearbySearchItem(
        hit: PlaceSearchGateway.SearchHit,
        savedPlace: Place?,
    ): PlaceResult.SearchItem {
        // 주변 검색은 지도 후보용 경량 shape만 조립하고 상세/사진/영업시간 계산은 피한다.
        val placeTypeSummary = fromRawTypes(hit.primaryType, hit.types)
            ?: toPlaceTypeSummary(savedPlace)
        return PlaceResult.SearchItem(
            placeId = savedPlace?.id,
            externalPlaceId = hit.externalPlaceId,
            placeName = hit.placeName,
            address = hit.address,
            latitude = hit.latitude,
            longitude = hit.longitude,
            placeTypeSummary = placeTypeSummary,
            normalizedCategoryKey = Companion.toNormalizedCategoryKey(placeTypeSummary)
                ?: toNormalizedCategoryKey(savedPlace),
            photoHint = null,
            placeDetailSummary = null,
            openingSummary = null,
        )
    }

    fun toDetail(place: Place): PlaceResult.Detail {
        // 상세 응답은 내부 Place와 detailSnapshot을 합쳐 단일 response shape 구성.
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
            priceLevel = place.detailSnapshot?.priceLevel,
            regularOpeningHoursJson = place.detailSnapshot?.regularOpeningHoursJson,
            currentOpeningHoursJson = place.detailSnapshot?.currentOpeningHoursJson,
        )
    }

    private fun PlaceSearchGateway.SearchHit.toDetailSummary(): PlaceDetailSummary? =
        if (businessStatus == null && rating == null && userRatingCount == null && editorialSummary == null) {
            null
        } else {
            PlaceDetailSummary(
                businessStatus = businessStatus,
                rating = rating,
                userRatingCount = userRatingCount,
                editorialSummary = editorialSummary,
            )
        }

    companion object {
        private val objectMapper = jacksonObjectMapper()

        fun fromRawTypes(primaryType: String?, types: List<String>): PlaceTypeSummary? {
            // Google type 정보가 전혀 없으면 분류 요약도 비움.
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
                // 저장된 JSON이 깨져도 목록/상세 응답은 빈 type으로 계속 조립.
                runCatching { objectMapper.readValue(it, Array<String>::class.java).toList() }.getOrDefault(emptyList())
            } ?: emptyList()

        fun toDisplayPrimaryLabel(primaryType: String?): String? =
            primaryType?.replace('_', ' ')

        fun normalizeCategory(
            primaryType: String?,
            types: List<String>,
        ): NormalizedPlaceCategoryKey? {
            // primaryType을 첫 후보로 두고 types 전체를 보조 후보로 병합.
            val candidates = buildList {
                primaryType?.let(::add)
                addAll(types)
            }.map { it.lowercase() }

            if (candidates.isEmpty()) {
                return null
            }

            // 더 구체적인 음식점/장소 유형을 먼저 매칭해 넓은 restaurant/store 분류보다 우선.
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
