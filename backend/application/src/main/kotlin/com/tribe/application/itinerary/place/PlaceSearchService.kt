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
        private const val MAX_RADIUS_METERS = 50_000
        private const val MIN_MAX_RESULT_COUNT = 1
        private const val MAX_MAX_RESULT_COUNT = 20
        private val SEARCH_CACHE_TTL: Duration = Duration.ofHours(6)
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
        val normalizedQuery = query?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val normalizedRegion = region
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) }
        val normalizedRadius = if (latitude != null && longitude != null) {
            (radiusMeters ?: MAX_RADIUS_METERS).coerceIn(1, MAX_RADIUS_METERS)
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
            context.regionContextKey ?: normalizedRegion.orEmpty(),
            latitude?.toString().orEmpty(),
            longitude?.toString().orEmpty(),
            normalizedRadius?.toString().orEmpty(),
        ).joinToString("|")

        val cached = placeSearchCacheRepository.get(cacheKey)
        if (cached != null) {
            return placeCatalogService.mergeWithCanonical(cached)
        }

        val results = placeSearchGateway.search(normalizedQuery, language, context)
        placeSearchCacheRepository.put(cacheKey, results, SEARCH_CACHE_TTL)
        return placeCatalogService.mergeWithCanonical(results)
    }

    fun searchNearby(
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Int?,
        maxResultCount: Int?,
        category: String?,
        language: String?,
        region: String?,
    ): List<PlaceResult.SearchItem> {
        val normalizedLatitude = validateLatitude(latitude)
        val normalizedLongitude = validateLongitude(longitude)
        val normalizedRadius = validateRadius(radiusMeters)
        val normalizedMaxResultCount = validateMaxResultCount(maxResultCount)
        val normalizedCategory = validateCategory(category)
        val normalizedLanguage = language?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "ko"
        val normalizedRegion = normalizeRegion(region)

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
            return toNearbySearchItems(cached)
        }

        val results = placeSearchGateway.searchNearby(request)
        placeSearchCacheRepository.put(cacheKey, results, SEARCH_CACHE_TTL)
        return toNearbySearchItems(results)
    }

    fun directions(originPlaceId: String, destinationPlaceId: String, mode: String): RouteDetails? {
        val normalized = mode.trim().uppercase()
        if (normalized !in setOf("WALKING", "DRIVING", "TRANSIT")) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, detail = mapOf("travelMode" to mode))
        }
        return placeSearchGateway.directions(originPlaceId, destinationPlaceId, normalized)
    }

    fun getPhoto(name: String, maxWidthPx: Int): PlacePhotoMedia =
        placeSearchGateway.getPhoto(name, maxWidthPx)
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

    @Transactional
    fun getPlaceDetail(placeId: Long, language: String = "ko"): PlaceResult.Detail {
        val place = placeRepository.findById(placeId)
            .orElseThrow { BusinessException(ErrorCode.PLACE_NOT_FOUND) }
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
        if (value == null || value <= 0 || value > MAX_RADIUS_METERS) {
            throw invalidNearbyInput("radiusMeters", value)
        }
        return value
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
            quantizeCoordinate(request.latitude),
            quantizeCoordinate(request.longitude),
        ).joinToString("|")
    }

    private fun toNearbySearchItems(results: List<PlaceSearchGateway.SearchHit>): List<PlaceResult.SearchItem> {
        return placeCatalogService.mergeWithCanonical(results)
            .map { it.copy(photoHint = null, placeDetailSummary = null) }
    }

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
