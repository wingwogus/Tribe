package com.tribe.application.itinerary.place

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.TimeoutException

/**
 * Google Places/Directions adapter.
 *
 * 외부 Google 응답을 application `PlaceSearchGateway` 계약으로 변환.
 */
@Component
@ConditionalOnProperty(name = ["tribe.itinerary.place-search.enabled"], havingValue = "true", matchIfMissing = true)
class GooglePlaceSearchGateway(
    private val webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
    @Value("\${google.maps.key}") private val apiKey: String,
    @Value("\${google.maps.timeout-ms:5000}") private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : PlaceSearchGateway {
    companion object {
        private const val MAX_RADIUS_METERS = 50_000
        private const val DEFAULT_TIMEOUT_MILLIS = 5_000L
        private val DETAILS_TIMEOUT = Duration.ofSeconds(5)
        private val DETAILS_RETRY_BACKOFF = Duration.ofMillis(200)
        internal const val SEARCH_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.primaryType,places.types,places.businessStatus,places.utcOffsetMinutes,places.rating,places.userRatingCount,places.currentOpeningHours,places.editorialSummary"
        internal const val NEARBY_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.primaryType,places.types"
        internal const val PLACE_SUMMARY_FIELD_MASK =
            "id,displayName,formattedAddress,location,primaryType,types"
        internal const val DETAILS_FIELD_MASK =
            "id,displayName,formattedAddress,location,primaryType,types,businessStatus,utcOffsetMinutes,nationalPhoneNumber,internationalPhoneNumber,websiteUri,googleMapsUri,rating,userRatingCount,priceLevel,regularOpeningHours,currentOpeningHours,photos,editorialSummary"
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient = webClientBuilder.build()
    private val placesTimeout: Duration = Duration.ofMillis(timeoutMillis)

    override fun search(query: String?, language: String, context: PlaceSearchContext): List<PlaceSearchGateway.SearchHit> {
        // 흐름: searchText body 조립 -> Google 호출 -> PlacesResponse를 SearchHit 후보로 축소.
        val body = buildSearchRequestBody(query, language, context) ?: return emptyList()
        val normalizedRegionCode = body["regionCode"] as? String
        val radiusMeters = ((body["locationBias"] as? Map<*, *>)?.get("circle") as? Map<*, *>)?.get("radius")

        val response = webClient.post()
            .uri("https://places.googleapis.com/v1/places:searchText")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", SEARCH_FIELD_MASK)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(PlacesResponse::class.java)
            .doOnError(WebClientResponseException::class.java) { ex ->
                logger.error(
                    "Google Places searchText failed: status={}, regionCode={}, radiusMeters={}, body={}",
                    ex.statusCode.value(),
                    normalizedRegionCode,
                    radiusMeters,
                    ex.responseBodyAsString,
                    ex,
                )
            }
            .doOnError { logger.error("Error calling Google Places API", it) }
            .block()
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

        return response.places?.map(::toSearchHit) ?: emptyList()
    }

    override fun searchNearby(request: PlaceSearchGateway.NearbySearchRequest): List<PlaceSearchGateway.SearchHit> {
        // 주변 검색은 application에서 검증된 request만 받아 Google Nearby body로 변환.
        val body = buildNearbySearchRequestBody(request)

        val response = webClient.post()
            .uri("https://places.googleapis.com/v1/places:searchNearby")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", NEARBY_FIELD_MASK)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(PlacesResponse::class.java)
            .timeout(placesTimeout)
            .doOnError(WebClientResponseException::class.java) { ex ->
                logger.error(
                    "Google Places searchNearby failed: status={}, category={}, radiusMeters={}, body={}",
                    ex.statusCode.value(),
                    request.category.name,
                    request.radiusMeters,
                    ex.responseBodyAsString,
                    ex,
                )
            }
            .doOnError(TimeoutException::class.java) {
                logger.error(
                    "Google Places searchNearby timed out: category={}, radiusMeters={}, timeoutMillis={}",
                    request.category.name,
                    request.radiusMeters,
                    timeoutMillis,
                    it,
                )
            }
            .onErrorMap(TimeoutException::class.java) { BusinessException(ErrorCode.EXTERNAL_API_ERROR) }
            .doOnError { logger.error("Error calling Google Places Nearby API", it) }
            .block()
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

        return response.places?.mapNotNull(::toNearbySearchHit) ?: emptyList()
    }

    override fun getPlaceSummary(externalPlaceId: String, language: String): PlaceSearchGateway.SearchHit? {
        // resolve/save 흐름에서 내부 Place를 만들 수 있는 최소 필드만 조회.
        val response = try {
            webClient.get()
                .uri { builder ->
                    builder
                        .scheme("https")
                        .host("places.googleapis.com")
                        .path("/v1/places/{placeId}")
                        .queryParam("languageCode", language)
                        .build(externalPlaceId)
                }
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", PLACE_SUMMARY_FIELD_MASK)
                .retrieve()
                .bodyToMono(PlaceDetailsResponse::class.java)
                .timeout(placesTimeout)
                .block()
        } catch (ex: WebClientResponseException) {
            if (ex.statusCode.value() == 404) {
                return null
            }
            logger.error(
                "Google Place Summary failed: status={}, body={}",
                ex.statusCode.value(),
                ex.responseBodyAsString,
                ex,
            )
            throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)
        } ?: return null

        return toSearchHit(response)
    }

    override fun getPlaceDetails(externalPlaceId: String, language: String): PlaceSearchGateway.DetailsPayload? {
        // 상세 조회는 snapshot 보강용 필드까지 포함한 별도 field mask 사용.
        val response = webClient.get()
            .uri { builder ->
                builder
                    .scheme("https")
                    .host("places.googleapis.com")
                    .path("/v1/places/{placeId}")
                    .queryParam("languageCode", language)
                    .build(externalPlaceId)
            }
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
            .retrieve()
            .bodyToMono(PlaceDetailsResponse::class.java)
            .timeout(DETAILS_TIMEOUT)
            .retryWhen(
                Retry.backoff(1, DETAILS_RETRY_BACKOFF)
                    .filter(::isTransientDetailsFailure)
                    .onRetryExhaustedThrow { _, signal -> signal.failure() },
            )
            .doOnError { ex ->
                if (isGoogleDetailsFailure(ex)) {
                    logger.warn(
                        "Google Place Details failed: externalPlaceId={}, status={}, retryable={}, cause={}",
                        externalPlaceId,
                        googleStatus(ex),
                        isTransientDetailsFailure(ex),
                        googleFailureCause(ex),
                    )
                }
            }
            .onErrorMap { ex ->
                if (isGoogleDetailsFailure(ex)) {
                    externalApiException(externalPlaceId, ex)
                } else {
                    ex
                }
            }
            .block()
            ?: return null

        return toDetailsPayload(response)
    }

    private fun isTransientDetailsFailure(ex: Throwable): Boolean =
        when (ex) {
            is WebClientRequestException,
            is TimeoutException,
            -> true
            is WebClientResponseException -> ex.statusCode.value() == 429 || ex.statusCode.is5xxServerError
            else -> false
        }

    private fun isGoogleDetailsFailure(ex: Throwable): Boolean =
        ex is WebClientRequestException || ex is TimeoutException || ex is WebClientResponseException

    private fun externalApiException(externalPlaceId: String, ex: Throwable): BusinessException {
        val detail = linkedMapOf<String, Any>(
            "operation" to "google_place_details",
            "externalPlaceId" to externalPlaceId,
        )
        googleStatus(ex)?.let { detail["status"] = it }
        detail["cause"] = googleFailureCause(ex)
        detail["retryable"] = isTransientDetailsFailure(ex)

        return BusinessException(
            ErrorCode.EXTERNAL_API_ERROR,
            detail = detail,
        )
    }

    private fun googleStatus(ex: Throwable): Int? =
        (ex as? WebClientResponseException)?.statusCode?.value()

    private fun googleFailureCause(ex: Throwable): String =
        when (ex) {
            is WebClientResponseException -> "http_status"
            is WebClientRequestException -> "request"
            is TimeoutException -> "timeout"
            else -> "unknown"
        }

    internal fun toDetailsPayload(response: PlaceDetailsResponse): PlaceSearchGateway.DetailsPayload {
        // Google 영업시간 JSON 원문은 UI 재해석 가능성을 위해 snapshot에 보관.
        val regularOpeningHoursJson = response.regularOpeningHours?.let { objectMapper.writeValueAsString(it) }
        val currentOpeningHoursJson = response.currentOpeningHours?.let { objectMapper.writeValueAsString(it) }

        val placeTypeSummary = PlaceResultAssembler.fromRawTypes(response.primaryType, response.types ?: emptyList())
        return PlaceSearchGateway.DetailsPayload(
            externalPlaceId = response.id,
            placeName = response.displayName?.text ?: "이름 없음",
            address = response.formattedAddress ?: "주소 정보 없음",
            latitude = response.location?.latitude ?: 0.0,
            longitude = response.location?.longitude ?: 0.0,
            primaryType = placeTypeSummary?.primaryType,
            types = placeTypeSummary?.types ?: emptyList(),
            businessStatus = response.businessStatus,
            utcOffsetMinutes = response.utcOffsetMinutes,
            formattedPhoneNumber = response.nationalPhoneNumber,
            internationalPhoneNumber = response.internationalPhoneNumber,
            websiteUri = response.websiteUri,
            googleMapsUri = response.googleMapsUri,
            rating = response.rating,
            userRatingCount = response.userRatingCount,
            priceLevel = parsePriceLevel(response.priceLevel),
            regularOpeningHoursJson = regularOpeningHoursJson,
            currentOpeningHoursJson = currentOpeningHoursJson,
            primaryPhotoName = response.photos?.firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) },
            editorialSummary = response.editorialSummary?.text,
            regularOpeningPeriods = parseRegularOpeningPeriods(response.regularOpeningHours),
        )
    }

    private fun toSearchOpeningSummary(result: PlacesResponse.PlaceResult): OpeningSummary? {
        val currentOpeningHours = result.currentOpeningHours ?: return null
        val openNow = currentOpeningHours.booleanFieldOrNull("openNow")
        val nextOpenTime = currentOpeningHours.textFieldOrNull("nextOpenTime")
        val nextCloseTime = currentOpeningHours.textFieldOrNull("nextCloseTime")
        if (openNow == null && nextOpenTime == null && nextCloseTime == null) {
            return null
        }

        return OpeningSummary(
            openNow = openNow,
            nextOpenTime = nextOpenTime,
            nextCloseTime = nextCloseTime,
            source = OpeningSummarySource.CURRENT,
            timezoneOffsetMinutes = result.utcOffsetMinutes,
            syncedAt = LocalDateTime.now(),
            stale = false,
        )
    }

    override fun getPhoto(photoName: String, maxWidthPx: Int): PlacePhotoMedia? {
        // Google photo media는 redirect URI만 받아 프론트에서 직접 사용 가능한 형태로 전달.
        return webClient.get()
            .uri("https://places.googleapis.com/v1/{photoName}/media?maxWidthPx={maxWidthPx}&skipHttpRedirect=true", photoName, maxWidthPx)
            .header("X-Goog-Api-Key", apiKey)
            .retrieve()
            .bodyToMono(PhotoMediaRedirectResponse::class.java)
            .map { response ->
                response.photoUri?.let { uri ->
                    PlacePhotoMedia(
                        redirectUri = uri,
                    )
                }
            }
            .doOnError { logger.error("Error calling Google Place Photo API", it) }
            .block()
    }

    override fun directions(originPlaceId: String, destinationPlaceId: String, travelMode: String): RouteDetails? {
        // Directions API는 legacy endpoint 사용, application에는 RouteDetails만 노출.
        val response = webClient.get()
            .uri { builder ->
                builder
                    .scheme("https")
                    .host("maps.googleapis.com")
                    .path("/maps/api/directions/json")
                    .queryParam("origin", "place_id:$originPlaceId")
                    .queryParam("destination", "place_id:$destinationPlaceId")
                    .queryParam("language", "ko")
                    .queryParam("mode", travelMode.lowercase())
                    .queryParam("key", apiKey)
                    .build()
            }
            .retrieve()
            .bodyToMono(DirectionsRawResponse::class.java)
            .doOnError { logger.error("Error calling Google Directions API", it) }
            .block()
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

        if (response.status != "OK") {
            // Google이 경로 없음/지원 불가를 반환하면 비즈니스 예외 대신 빈 경로 의미 유지.
            return null
        }

        val route = response.routes.firstOrNull() ?: return null
        val leg = route.legs.firstOrNull() ?: return null
        // Directions 응답의 이름/주소를 다시 searchText로 보강, 실패 시 placeId 기반 placeholder 유지.
        val origin = searchRoutePlaceByName(route.originName)
            ?: PlaceSearchGateway.SearchHit(
                externalPlaceId = originPlaceId,
                placeName = route.originName ?: "출발지",
                address = route.originAddress ?: "",
                latitude = 0.0,
                longitude = 0.0,
            )
        val destination = searchRoutePlaceByName(route.destinationName)
            ?: PlaceSearchGateway.SearchHit(
                externalPlaceId = destinationPlaceId,
                placeName = route.destinationName ?: "도착지",
                address = route.destinationAddress ?: "",
                latitude = 0.0,
                longitude = 0.0,
            )

        return RouteDetails(
            travelMode = travelMode,
            originPlace = origin,
            destinationPlace = destination,
            totalDuration = leg.duration?.text ?: "",
            totalDistance = leg.distance?.text ?: "",
            steps = leg.steps.map { rawStep ->
                RouteDetails.RouteStep(
                    travelMode = rawStep.travelMode ?: "",
                    instructions = rawStep.htmlInstructions?.replace(Regex("<[^>]*>"), "") ?: "",
                    duration = rawStep.duration?.text ?: "",
                    distance = rawStep.distance?.text ?: "",
                    transitDetails = rawStep.transitDetails?.let { transit ->
                        RouteDetails.TransitDetails(
                            lineName = transit.line?.shortName ?: "이름 없음",
                            vehicleType = transit.line?.vehicle?.type ?: "",
                            vehicleIconUrl = transit.line?.vehicle?.icon,
                            numStops = transit.numStops ?: 0,
                            departureStop = transit.departureStop?.name ?: "",
                            arrivalStop = transit.arrivalStop?.name ?: "",
                        )
                    },
                )
            },
        )
    }

    internal fun buildSearchRequestBody(
        query: String?,
        language: String,
        context: PlaceSearchContext,
    ): Map<String, Any>? {
        // 빈 검색어는 Google 호출 자체를 만들지 않는 계약.
        val normalizedQuery = query?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalizedRegionCode = context.regionCode
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) }

        return buildMap<String, Any> {
            put("textQuery", normalizedQuery)
            put("languageCode", language)
            normalizedRegionCode?.let { put("regionCode", it) }
            if (context.latitude != null && context.longitude != null) {
                // 텍스트 검색은 locationBias로만 위치 선호도 반영, hard restriction 아님.
                val radius = (context.radiusMeters ?: MAX_RADIUS_METERS).coerceIn(1, MAX_RADIUS_METERS)
                put(
                    "locationBias",
                    mapOf(
                        "circle" to mapOf(
                            "center" to mapOf(
                                "latitude" to context.latitude,
                                "longitude" to context.longitude,
                            ),
                            "radius" to radius,
                        ),
                    ),
                )
            }
        }
    }

    internal fun buildNearbySearchRequestBody(
        request: PlaceSearchGateway.NearbySearchRequest,
    ): Map<String, Any> {
        // 주변 검색은 category별 includedTypes와 지도 중심 원형 제한을 함께 전달.
        return buildMap {
            put("includedTypes", googleIncludedTypesFor(request.category))
            put("maxResultCount", request.maxResultCount)
            put("languageCode", request.language)
            put("rankPreference", "DISTANCE")
            request.region?.let { put("regionCode", it) }
            put(
                "locationRestriction",
                mapOf(
                    "circle" to mapOf(
                        "center" to mapOf(
                            "latitude" to request.latitude,
                            "longitude" to request.longitude,
                        ),
                        "radius" to request.radiusMeters,
                    ),
                ),
            )
        }
    }

    // 앱 카테고리를 Google Places includedTypes로 변환.
    internal fun googleIncludedTypesFor(category: NearbyPlaceCategory): List<String> = when (category) {
        NearbyPlaceCategory.RESTAURANT -> listOf("restaurant")
        NearbyPlaceCategory.CAFE -> listOf("cafe", "coffee_shop")
        NearbyPlaceCategory.BAKERY -> listOf("bakery")
        NearbyPlaceCategory.BAR -> listOf("bar", "pub")
        NearbyPlaceCategory.ATTRACTION -> listOf("tourist_attraction")
        NearbyPlaceCategory.SHOPPING -> listOf("shopping_mall", "department_store", "market", "store")
        NearbyPlaceCategory.PARK -> listOf("park")
        NearbyPlaceCategory.MUSEUM -> listOf("museum", "art_gallery")
        NearbyPlaceCategory.STAY -> listOf("hotel", "hostel", "lodging", "resort_hotel")
    }

    private fun searchRoutePlaceByName(name: String?): PlaceSearchGateway.SearchHit? {
        // Directions에 external placeId가 없을 때 이름 검색으로 표시용 좌표 보강.
        val normalizedName = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return search(normalizedName, "ko", PlaceSearchContext(regionCode = null)).firstOrNull()
    }

    // Google price enum을 앱의 숫자 단계로 축소.
    internal fun parsePriceLevel(priceLevel: String?): Int? = when (priceLevel) {
        null, "PRICE_LEVEL_UNSPECIFIED" -> null
        "PRICE_LEVEL_FREE" -> 0
        "PRICE_LEVEL_INEXPENSIVE" -> 1
        "PRICE_LEVEL_MODERATE" -> 2
        "PRICE_LEVEL_EXPENSIVE" -> 3
        "PRICE_LEVEL_VERY_EXPENSIVE" -> 4
        else -> null
    }

    private fun toSearchHit(place: PlacesResponse.PlaceResult): PlaceSearchGateway.SearchHit {
        // 목록 응답의 Google place를 application 공통 후보 shape로 축소.
        val placeTypeSummary = PlaceResultAssembler.fromRawTypes(place.primaryType, place.types ?: emptyList())
        return PlaceSearchGateway.SearchHit(
            externalPlaceId = place.id,
            placeName = place.displayName?.text ?: "이름 없음",
            address = place.formattedAddress ?: "주소 정보 없음",
            latitude = place.location?.latitude ?: 0.0,
            longitude = place.location?.longitude ?: 0.0,
            primaryType = placeTypeSummary?.primaryType,
            types = placeTypeSummary?.types ?: emptyList(),
            businessStatus = place.businessStatus,
            rating = place.rating,
            userRatingCount = place.userRatingCount,
            editorialSummary = place.editorialSummary?.text,
            openingSummary = toSearchOpeningSummary(place),
        )
    }

    internal fun toNearbySearchHit(place: PlacesResponse.PlaceResult): PlaceSearchGateway.SearchHit? {
        // 주변 검색은 좌표 없는 후보를 목록에서 제외.
        val location = place.location ?: return null
        val placeTypeSummary = PlaceResultAssembler.fromRawTypes(place.primaryType, place.types ?: emptyList())
        return PlaceSearchGateway.SearchHit(
            externalPlaceId = place.id,
            placeName = place.displayName?.text ?: "이름 없음",
            address = place.formattedAddress ?: "주소 정보 없음",
            latitude = location.latitude,
            longitude = location.longitude,
            primaryType = placeTypeSummary?.primaryType,
            types = placeTypeSummary?.types ?: emptyList(),
        )
    }

    private fun toSearchHit(place: PlaceDetailsResponse): PlaceSearchGateway.SearchHit {
        // summary/details 응답도 검색 후보와 같은 SearchHit shape 재사용.
        val placeTypeSummary = PlaceResultAssembler.fromRawTypes(place.primaryType, place.types ?: emptyList())
        return PlaceSearchGateway.SearchHit(
            externalPlaceId = place.id,
            placeName = place.displayName?.text ?: "이름 없음",
            address = place.formattedAddress ?: "주소 정보 없음",
            latitude = place.location?.latitude ?: 0.0,
            longitude = place.location?.longitude ?: 0.0,
            primaryType = placeTypeSummary?.primaryType,
            types = placeTypeSummary?.types ?: emptyList(),
            businessStatus = place.businessStatus,
            rating = place.rating,
            userRatingCount = place.userRatingCount,
            editorialSummary = place.editorialSummary?.text,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PlacesResponse(
        val places: List<PlaceResult>?,
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class PlaceResult(
            val id: String,
            val formattedAddress: String?,
            val location: Location?,
            val displayName: DisplayName?,
            val primaryType: String?,
            val types: List<String>?,
            val businessStatus: String? = null,
            val utcOffsetMinutes: Int? = null,
            val rating: Double? = null,
            val userRatingCount: Int? = null,
            val currentOpeningHours: JsonNode? = null,
            val editorialSummary: DisplayName? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Location(
            val latitude: Double,
            val longitude: Double,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class DisplayName(
            val text: String,
            val languageCode: String?,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PlaceDetailsResponse(
        val id: String,
        val formattedAddress: String?,
        val location: PlacesResponse.Location?,
        val displayName: PlacesResponse.DisplayName?,
        val primaryType: String?,
        val types: List<String>?,
        val businessStatus: String?,
        val utcOffsetMinutes: Int?,
        val nationalPhoneNumber: String?,
        val internationalPhoneNumber: String?,
        val websiteUri: String?,
        val googleMapsUri: String?,
        val rating: Double?,
        val userRatingCount: Int?,
        val priceLevel: String?,
        val regularOpeningHours: JsonNode?,
        val currentOpeningHours: JsonNode?,
        val photos: List<Photo>?,
        val editorialSummary: PlacesResponse.DisplayName?,
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Photo(
            val name: String?,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PhotoMediaRedirectResponse(
        val photoUri: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DirectionsRawResponse(
        val status: String,
        val routes: List<Route> = emptyList(),
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Route(
            val legs: List<Leg> = emptyList(),
            val summary: String? = null,
            val copyrights: String? = null,
            val warnings: List<String> = emptyList(),
            val waypointOrder: List<Int> = emptyList(),
            val overviewPolyline: Polyline? = null,
            val bounds: Map<String, Any>? = null,
            val fare: Map<String, Any>? = null,
            val originAddress: String? = null,
            val destinationAddress: String? = null,
            val originName: String? = null,
            val destinationName: String? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Polyline(val points: String? = null)

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Leg(
            val distance: TextValue? = null,
            val duration: TextValue? = null,
            val steps: List<Step> = emptyList(),
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Step(
            val travelMode: String? = null,
            val htmlInstructions: String? = null,
            val distance: TextValue? = null,
            val duration: TextValue? = null,
            val transitDetails: TransitDetails? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class TextValue(
            val text: String? = null,
            val value: Long? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class TransitDetails(
            val departureStop: Stop? = null,
            val arrivalStop: Stop? = null,
            val numStops: Int? = null,
            val line: Line? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Stop(
            val name: String? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Line(
            val shortName: String? = null,
            val vehicle: Vehicle? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Vehicle(
            val type: String? = null,
            val icon: String? = null,
        )
    }

    private fun parseRegularOpeningPeriods(regularOpeningHours: JsonNode?): List<PlaceSearchGateway.RegularOpeningPeriodInput> {
        val periods = regularOpeningHours?.get("periods") ?: return emptyList()
        if (!periods.isArray) return emptyList()

        return periods.mapIndexedNotNull { index, node ->
            val open = node.objectFieldOrNull("open") ?: return@mapIndexedNotNull null
            val close = node.objectFieldOrNull("close") ?: return@mapIndexedNotNull null
            val openDay = open.intFieldOrNull("day", 0..6) ?: return@mapIndexedNotNull null
            val openHour = open.intFieldOrNull("hour", 0..23, defaultValue = 0) ?: return@mapIndexedNotNull null
            val openMinute = open.intFieldOrNull("minute", 0..59, defaultValue = 0) ?: return@mapIndexedNotNull null
            val closeDay = close.intFieldOrNull("day", 0..6, defaultValue = openDay) ?: return@mapIndexedNotNull null
            val closeHour = close.intFieldOrNull("hour", 0..23, defaultValue = 0) ?: return@mapIndexedNotNull null
            val closeMinute = close.intFieldOrNull("minute", 0..59, defaultValue = 0) ?: return@mapIndexedNotNull null
            val openTotal = openHour * 60 + openMinute
            val closeTotal = closeHour * 60 + closeMinute

            PlaceSearchGateway.RegularOpeningPeriodInput(
                dayOfWeek = openDay,
                openMinute = openTotal,
                closeMinute = closeTotal,
                isOvernight = closeDay != openDay || closeTotal < openTotal,
                sequenceNo = index + 1,
            )
        }
    }

    private fun JsonNode.objectFieldOrNull(fieldName: String): JsonNode? =
        get(fieldName)?.takeIf { it.isObject }

    private fun JsonNode.booleanFieldOrNull(fieldName: String): Boolean? {
        val value = get(fieldName) ?: return null
        return value.takeIf { it.isBoolean }?.booleanValue()
    }

    private fun JsonNode.textFieldOrNull(fieldName: String): String? {
        val value = get(fieldName) ?: return null
        return value.takeIf { it.isTextual }?.asText()?.takeIf(String::isNotBlank)
    }

    private fun JsonNode.intFieldOrNull(
        fieldName: String,
        range: IntRange,
        defaultValue: Int? = null,
    ): Int? {
        val value = get(fieldName) ?: return defaultValue
        if (!value.isIntegralNumber || !value.canConvertToInt()) return null
        return value.intValue().takeIf { it in range }
    }
}
