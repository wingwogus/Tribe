package com.tribe.api.itinerary.place

import com.tribe.api.common.ApiResponse
import com.tribe.application.itinerary.place.PlaceSearchService
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * 장소 HTTP API.
 *
 * 검색 후보 조회와 내부 Place 확정/상세 조회 endpoint 분리.
 */
@RestController
@RequestMapping("/api/v1/places")
class PlaceController(
    private val placeSearchService: PlaceSearchService,
) {
    @GetMapping("/search")
    fun searchPlaces(
        @RequestParam query: String?,
        @RequestParam region: String?,
        @RequestParam(defaultValue = "ko") language: String,
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(required = false) radiusMeters: Int?,
        @RequestParam(required = false) regionContextKey: String?,
    ): ResponseEntity<ApiResponse<List<PlaceResponses.SearchResponse>>> {
        // 텍스트 검색은 후보 조회 전용, 내부 Place 저장은 별도 resolve 흐름.
        val result = placeSearchService.search(query, language, region, latitude, longitude, radiusMeters, regionContextKey)
            .map(PlaceResponses.SearchResponse::from)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @PostMapping("/nearby")
    fun searchNearbyPlaces(
        @RequestBody request: PlaceRequests.NearbySearchRequest,
    ): ResponseEntity<ApiResponse<List<PlaceResponses.SearchResponse>>> {
        // 지도 중심 주변 검색은 request body로 좌표/반경/category를 받아 application 검증에 위임.
        val result = placeSearchService.searchNearby(
            latitude = request.latitude,
            longitude = request.longitude,
            radiusMeters = request.radiusMeters,
            maxResultCount = request.maxResultCount,
            category = request.category,
            language = request.language,
            region = request.region,
        ).map(PlaceResponses.SearchResponse::fromNearby)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @PostMapping("/resolve")
    fun resolveExternalPlace(
        @RequestBody request: PlaceRequests.ResolveExternalPlaceRequest,
    ): ResponseEntity<ApiResponse<PlaceResponses.SearchResponse>> {
        // 클라이언트가 선택한 외부 후보를 내부 canonical Place로 확정.
        val result = placeSearchService.resolveExternalPlace(
            externalPlaceId = request.externalPlaceId,
            language = request.language,
        )
        return ResponseEntity.ok(ApiResponse.ok(PlaceResponses.SearchResponse.from(result)))
    }

    @GetMapping("/{placeId}")
    fun getPlaceDetail(
        @PathVariable placeId: Long,
        @RequestParam(defaultValue = "ko") language: String,
    ): ResponseEntity<ApiResponse<PlaceResponses.DetailResponse>> {
        // 상세 조회는 내부 placeId 기준, 필요 시 application에서 Google details 보강.
        val detail = placeSearchService.getPlaceDetail(placeId, language)
        return ResponseEntity.ok(
            ApiResponse.ok(PlaceResponses.DetailResponse.from(detail)),
        )
    }

    @GetMapping("/photos")
    fun getPlacePhoto(
        @RequestParam name: String,
        @RequestParam(defaultValue = "320") maxWidthPx: Int,
    ): ResponseEntity<*> {
        // Google photo redirect URI가 있으면 302로 연결, binary media면 image body 반환.
        val media = placeSearchService.getPhoto(name, maxWidthPx)
        media.redirectUri?.let { redirectUri ->
            return ResponseEntity.status(302)
                .location(URI.create(redirectUri))
                .build<Any>()
        }
        return ResponseEntity.ok()
            .contentType(media.contentType?.let(MediaType::parseMediaType) ?: MediaType.IMAGE_JPEG)
            .body(ByteArrayResource(media.bytes ?: ByteArray(0)))
    }
}
