package com.tribe.application.itinerary.place

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.itinerary.place.PlaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.Locale

/**
 * 장소 검색 use case.
 *
 * 검색: 외부 후보 조회 + 캐시 + 저장된 canonical 장소 병합.
 * 저장: `/resolve` 또는 `PlaceCatalogService`를 통한 별도 확정.
 */
@Service
@Transactional(readOnly = true)
class PlaceSearchService(
    private val placeSearchGateway: PlaceSearchGateway,
    private val placeSearchCacheRepository: PlaceSearchCacheRepository,
    private val placeCatalogService: PlaceCatalogService,
    private val placeRepository: PlaceRepository,
    private val placeResultAssembler: PlaceResultAssembler,
) {
    companion object {
        private const val MAX_TEXT_SEARCH_RADIUS_METERS = 50_000
        private const val MAX_NEARBY_RADIUS_METERS = 5_000
        private const val NEARBY_RADIUS_BUCKET_METERS = 100
        private const val MAX_LANGUAGE_TAG_LENGTH = 20
        private const val MIN_MAX_RESULT_COUNT = 1
        private const val MAX_MAX_RESULT_COUNT = 20
        private val SEARCH_CACHE_TTL: Duration = Duration.ofHours(6)
        private val LANGUAGE_TAG_PATTERN = Regex("^[a-zA-Z]{2,3}(?:-[a-zA-Z0-9]{2,8}){0,2}$")
    }

    fun search(
        query: String?,
        language: String,
        region: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Int? = null,
        regionContextKey: String? = null,
    ): List<PlaceResult.SearchItem> {
        // 흐름: 입력 정규화 -> 캐시 조회 -> Google searchText -> canonical 병합 순서 확정.
        // 빈 검색어는 외부 호출 없이 빈 목록 처리.
        val normalizedQuery = query?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
        // Google regionCode는 2자리 국가 코드만 허용.
        val normalizedRegion = region
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) }
        // 좌표가 있을 때만 텍스트 검색 위치 편향 적용.
        val normalizedRadius = if (latitude != null && longitude != null) {
            (radiusMeters ?: MAX_TEXT_SEARCH_RADIUS_METERS).coerceIn(1, MAX_TEXT_SEARCH_RADIUS_METERS)
        } else {
            null
        }
        val context = PlaceSearchContext(
            regionCode = normalizedRegion,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = normalizedRadius,
            regionContextKey = regionContextKey,
        )
        val cacheKey = listOf(
            normalizedQuery.lowercase(),
            language.lowercase(),
            // 검색어가 같아도 지역/좌표 문맥이 다르면 별도 캐시.
            context.regionContextKey ?: normalizedRegion.orEmpty(),
            latitude?.toString().orEmpty(),
            longitude?.toString().orEmpty(),
            normalizedRadius?.toString().orEmpty(),
        ).joinToString("|")

        val cached = placeSearchCacheRepository.get(cacheKey)
        if (cached != null) {
            // 캐시된 외부 후보도 최신 canonical 정보와 다시 병합해 placeId 최신성 확보.
            return placeCatalogService.mergeWithCanonical(cached)
        }

        // 외부 검색 결과는 SearchHit 후보로만 캐시, 내부 저장은 별도 확정 흐름.
        val results = placeSearchGateway.search(normalizedQuery, language, context)
        placeSearchCacheRepository.put(cacheKey, results, SEARCH_CACHE_TTL)
        return placeCatalogService.mergeWithCanonical(results)
    }

    /**
     * 지도 중심 좌표 기준 주변 장소 후보 조회.
     *
     * 입력 검증, 외부 요청 정규화, 캐시 병합까지 한 use case.
     */
    fun searchNearby(
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Int?,
        maxResultCount: Int?,
        category: String?,
        language: String?,
        region: String?,
    ): List<PlaceResult.SearchItem> {
        // 흐름: 좌표/반경/category 검증 -> 캐시 조회 -> Google searchNearby -> 경량 응답 조립.
        // 주변 검색은 필수 좌표/반경/카테고리를 먼저 application 규칙으로 검증.
        val normalizedLatitude = validateLatitude(latitude)
        val normalizedLongitude = validateLongitude(longitude)
        val normalizedRadius = normalizeNearbyRadius(validateRadius(radiusMeters))
        val normalizedMaxResultCount = validateMaxResultCount(maxResultCount)
        val normalizedCategory = validateCategory(category)
        // 언어 미지정 시 한국어 검색 결과 우선.
        val normalizedLanguage = validateLanguage(language)
        val normalizedRegion = normalizeRegion(region)

        // gateway에는 검증된 NearbySearchRequest만 전달.
        val request = PlaceSearchGateway.NearbySearchRequest(
            latitude = normalizedLatitude,
            longitude = normalizedLongitude,
            radiusMeters = normalizedRadius,
            maxResultCount = normalizedMaxResultCount,
            category = normalizedCategory,
            language = normalizedLanguage,
            region = normalizedRegion,
        )
        val cacheKey = nearbyCacheKey(request)

        val cached = placeSearchCacheRepository.get(cacheKey)
        if (cached != null) {
            // 주변 검색 캐시도 저장된 장소 정보와 재병합 후 목록 전용 shape로 경량화.
            return toNearbySearchItems(cached)
        }

        // Nearby API 결과는 목록 표시용 후보이며 즉시 저장하지 않는 흐름.
        val results = placeSearchGateway.searchNearby(request)
        placeSearchCacheRepository.put(cacheKey, results, SEARCH_CACHE_TTL)
        return toNearbySearchItems(results)
    }

    fun directions(originPlaceId: String, destinationPlaceId: String, mode: String): RouteDetails? {
        // 흐름: travelMode 검증 -> Google Directions 호출 -> 경로 없음은 null 유지.
        // API 입력 문자열을 Google Directions 지원 모드로 제한.
        val normalized = mode.trim().uppercase()
        if (normalized !in setOf("WALKING", "DRIVING", "TRANSIT")) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, detail = mapOf("travelMode" to mode))
        }
        return placeSearchGateway.directions(originPlaceId, destinationPlaceId, normalized)
    }

    /**
     * 외부 장소 ID만으로 내부 `Place` 확보.
     *
     * 기존 저장분 재사용, 없으면 외부 summary 기반 최소 정보 저장.
     */
    @Transactional
    fun resolveExternalPlace(externalPlaceId: String?, language: String? = "ko"): PlaceResult.SearchItem {
        // 흐름: externalPlaceId 검증 -> 기존 Place 재사용/summary 저장 -> 검색 응답 shape 조립.
        // 클라이언트가 선택한 외부 후보 ID의 유효성 확정.
        val normalizedExternalPlaceId = externalPlaceId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw invalidNearbyInput("externalPlaceId", externalPlaceId)
        val normalizedLanguage = language?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "ko"

        // 저장된 장소 재사용 또는 summary 조회 기반 신규 Place 생성.
        val place = placeCatalogService.getOrCreateFromExternalPlaceId(
            externalPlaceId = normalizedExternalPlaceId,
            language = normalizedLanguage,
        )
        // resolve 응답은 검색 응답 모델과 동일하게 내려 UI 재사용성 확보.
        return placeResultAssembler.toSearchItem(
            PlaceSearchGateway.SearchHit(
                externalPlaceId = place.externalPlaceId,
                placeName = place.name,
                address = place.address ?: "주소 정보 없음",
                latitude = place.latitude.toDouble(),
                longitude = place.longitude.toDouble(),
            ),
            place,
        )
    }

    fun getPhoto(name: String, maxWidthPx: Int): PlacePhotoMedia =
        placeSearchGateway.getPhoto(name, maxWidthPx)
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

    /**
     * 내부 `Place` 상세 조회.
     *
     * 상세 정보가 없으면 조회 시점에 Google details 동기화.
     */
    @Transactional
    fun getPlaceDetail(placeId: Long, language: String = "ko"): PlaceResult.Detail {
        // 흐름: 내부 placeId 조회 -> details 미동기화 확인 -> snapshot/영업시간 보강.
        // 상세 조회는 내부 placeId만 허용.
        val place = placeRepository.findById(placeId)
            .orElseThrow { BusinessException(ErrorCode.PLACE_NOT_FOUND) }
        // detailsSyncedAt이 없을 때만 외부 상세 정보 보강.
        placeCatalogService.enrichDetailsIfNeeded(place, language)
        return placeResultAssembler.toDetail(place)
    }

    private fun validateLatitude(value: Double?): Double {
        if (value == null || !value.isFinite() || value < -90.0 || value > 90.0) {
            throw invalidNearbyInput("latitude", value)
        }
        return value
    }

    private fun validateLongitude(value: Double?): Double {
        if (value == null || !value.isFinite() || value < -180.0 || value > 180.0) {
            throw invalidNearbyInput("longitude", value)
        }
        return value
    }

    private fun validateRadius(value: Int?): Int {
        if (value == null || value <= 0 || value > MAX_NEARBY_RADIUS_METERS) {
            throw invalidNearbyInput("radiusMeters", value)
        }
        return value
    }

    private fun normalizeNearbyRadius(value: Int): Int {
        return (((value + NEARBY_RADIUS_BUCKET_METERS - 1) / NEARBY_RADIUS_BUCKET_METERS) * NEARBY_RADIUS_BUCKET_METERS)
            .coerceAtMost(MAX_NEARBY_RADIUS_METERS)
    }

    private fun validateMaxResultCount(value: Int?): Int {
        if (value == null || value !in MIN_MAX_RESULT_COUNT..MAX_MAX_RESULT_COUNT) {
            throw invalidNearbyInput("maxResultCount", value)
        }
        return value
    }

    private fun validateCategory(value: String?): NearbyPlaceCategory {
        val normalized = value?.trim()?.uppercase(Locale.ROOT)
        return NearbyPlaceCategory.entries.firstOrNull { it.name == normalized }
            ?: throw invalidNearbyInput("category", value)
    }

    private fun validateLanguage(value: String?): String {
        val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return "ko"
        if (normalized.length > MAX_LANGUAGE_TAG_LENGTH || !LANGUAGE_TAG_PATTERN.matches(normalized)) {
            throw invalidNearbyInput("language", value)
        }
        return normalized.lowercase(Locale.ROOT)
    }

    private fun normalizeRegion(value: String?): String? {
        return value
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) }
    }

    private fun nearbyCacheKey(request: PlaceSearchGateway.NearbySearchRequest): String {
        return listOf(
            "nearby:v1",
            request.category.name,
            request.language,
            request.region.orEmpty(),
            request.radiusMeters.toString(),
            request.maxResultCount.toString(),
            // 화면 좌표 흔들림을 줄이는 4자리 반올림.
            quantizeCoordinate(request.latitude),
            quantizeCoordinate(request.longitude),
        ).joinToString("|")
    }

    private fun toNearbySearchItems(results: List<PlaceSearchGateway.SearchHit>): List<PlaceResult.SearchItem> {
        return placeCatalogService.mergeWithCanonical(results)
            // 주변 목록 응답은 상세 요약 제거로 경량화.
            .map { it.copy(photoHint = null, placeDetailSummary = null) }
    }

    /**
     * 주변 검색 캐시 키용 좌표 정규화.
     */
    private fun quantizeCoordinate(value: Double): String {
        return BigDecimal.valueOf(value)
            .setScale(4, RoundingMode.HALF_UP)
            .toPlainString()
    }

    private fun invalidNearbyInput(field: String, rejectedValue: Any?): BusinessException =
        BusinessException(
            ErrorCode.INVALID_INPUT,
            detail = mapOf(
                "field" to field,
                "rejectedValue" to rejectedValue,
            ),
        )
}
