package com.tribe.api.itinerary.place

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.NearbyPlaceCategory
import com.tribe.application.itinerary.place.PlacePhotoMedia
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.itinerary.place.PlaceSearchContext
import com.tribe.application.itinerary.place.PlaceSearchGateway
import com.tribe.application.itinerary.place.RouteDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.Locale

@Component
@ConditionalOnProperty(name = ["tribe.itinerary.place-search.enabled"], havingValue = "true", matchIfMissing = true)
class GooglePlaceSearchGateway(
    private val webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
    @Value("\${google.maps.key}") private val apiKey: String,
) : PlaceSearchGateway {
    companion object {
        private const val MAX_RADIUS_METERS = 50_000
        internal const val NEARBY_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.primaryType,places.types"
        internal const val PLACE_SUMMARY_FIELD_MASK =
            "id,displayName,formattedAddress,location,primaryType,types"
        private const val PLACE_DETAILS_FIELD_MASK =
            "id,displayName,formattedAddress,location,primaryType,types,businessStatus,utcOffsetMinutes,nationalPhoneNumber,internationalPhoneNumber,websiteUri,googleMapsUri,rating,userRatingCount,priceLevel,regularOpeningHours,currentOpeningHours,editorialSummary"
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient = webClientBuilder.build()

    override fun search(query: String?, language: String, context: PlaceSearchContext): List<PlaceSearchGateway.SearchHit> {
        val body = buildSearchRequestBody(query, language, context) ?: return emptyList()
        val normalizedRegionCode = body["regionCode"] as? String
        val radiusMeters = ((body["locationBias"] as? Map<*, *>)?.get("circle") as? Map<*, *>)?.get("radius")

        val response = webClient.post()
            .uri("https://places.googleapis.com/v1/places:searchText")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", NEARBY_FIELD_MASK)
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

        return response.places?.map {
            toSearchHit(it)
        } ?: emptyList()
    }

    override fun searchNearby(request: PlaceSearchGateway.NearbySearchRequest): List<PlaceSearchGateway.SearchHit> {
        val body = buildNearbySearchRequestBody(request)

        val response = webClient.post()
            .uri("https://places.googleapis.com/v1/places:searchNearby")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", NEARBY_FIELD_MASK)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(PlacesResponse::class.java)
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
            .doOnError { logger.error("Error calling Google Places Nearby API", it) }
            .block()
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

        return response.places?.map { toSearchHit(it) } ?: emptyList()
    }

    override fun getPlaceSummary(externalPlaceId: String, language: String): PlaceSearchGateway.SearchHit? {
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
            .header("X-Goog-FieldMask", PLACE_DETAILS_FIELD_MASK)
            .retrieve()
            .bodyToMono(PlaceDetailsResponse::class.java)
            .doOnError { logger.error("Error calling Google Place Details API", it) }
            .block()
            ?: return null

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
            primaryPhotoName = null,
            editorialSummary = response.editorialSummary?.text,
            regularOpeningPeriods = parseRegularOpeningPeriods(response.regularOpeningHours),
        )
    }

    override fun getPhoto(photoName: String, maxWidthPx: Int): PlacePhotoMedia? {
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
            return null
        }

        val route = response.routes.firstOrNull() ?: return null
        val leg = route.legs.firstOrNull() ?: return null
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
        val normalizedName = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return search(normalizedName, "ko", PlaceSearchContext(regionCode = null)).firstOrNull()
    }

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
        val placeTypeSummary = PlaceResultAssembler.fromRawTypes(place.primaryType, place.types ?: emptyList())
        return PlaceSearchGateway.SearchHit(
            externalPlaceId = place.id,
            placeName = place.displayName?.text ?: "이름 없음",
            address = place.formattedAddress ?: "주소 정보 없음",
            latitude = place.location?.latitude ?: 0.0,
            longitude = place.location?.longitude ?: 0.0,
            primaryType = placeTypeSummary?.primaryType,
            types = placeTypeSummary?.types ?: emptyList(),
        )
    }

    private fun toSearchHit(place: PlaceDetailsResponse): PlaceSearchGateway.SearchHit {
        val placeTypeSummary = PlaceResultAssembler.fromRawTypes(place.primaryType, place.types ?: emptyList())
        return PlaceSearchGateway.SearchHit(
            externalPlaceId = place.id,
            placeName = place.displayName?.text ?: "이름 없음",
            address = place.formattedAddress ?: "주소 정보 없음",
            latitude = place.location?.latitude ?: 0.0,
            longitude = place.location?.longitude ?: 0.0,
            primaryType = placeTypeSummary?.primaryType,
            types = placeTypeSummary?.types ?: emptyList(),
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
        val editorialSummary: PlacesResponse.DisplayName?
    )

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
            val open = node.get("open") ?: return@mapIndexedNotNull null
            val close = node.get("close")
            val openDay = open.get("day")?.asInt() ?: return@mapIndexedNotNull null
            val openHour = open.get("hour")?.asInt() ?: 0
            val openMinute = open.get("minute")?.asInt() ?: 0
            val closeDay = close?.get("day")?.asInt() ?: openDay
            val closeHour = close?.get("hour")?.asInt() ?: openHour
            val closeMinute = close?.get("minute")?.asInt() ?: openMinute
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
}
