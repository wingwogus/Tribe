package com.tribe.application.itinerary.place

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.place.PlaceDetailSnapshotRepository
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriodRepository
import com.tribe.domain.itinerary.place.PlaceRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 장소 canonical catalog use case.
 *
 * 외부 Google placeId를 내부 `Place`로 고정하고 상세 snapshot을 보강하는 경계.
 */
@Service
@Transactional
class PlaceCatalogService(
    private val objectMapper: ObjectMapper,
    private val placeResultAssembler: PlaceResultAssembler,
    private val placeRepository: PlaceRepository,
    private val detailSnapshotRepository: PlaceDetailSnapshotRepository,
    private val openingPeriodRepository: PlaceRegularOpeningPeriodRepository,
    private val placeSearchGateway: PlaceSearchGateway,
    private val transactionManager: PlatformTransactionManager,
) {
    fun findExistingPlaces(results: List<PlaceSearchGateway.SearchHit>): Map<String, Place> {
        // 외부 후보 목록에서 이미 저장된 canonical Place만 한 번에 조회.
        if (results.isEmpty()) return emptyMap()
        return placeRepository.findByExternalPlaceIdIn(results.map { it.externalPlaceId }).associateBy { it.externalPlaceId }
    }

    fun getOrCreateAndEnrich(
        externalPlaceId: String,
        placeName: String,
        address: String?,
        latitude: BigDecimal,
        longitude: BigDecimal,
        language: String = "ko",
    ): Place {
        // 흐름: 기존 Place 조회 -> 없으면 새 canonical 저장 -> 상세 정보 보강.
        val place = placeRepository.findByExternalPlaceId(externalPlaceId)
            ?: createPlaceOrFindConcurrent(
                externalPlaceId = externalPlaceId,
                placeName = placeName,
                address = address,
                latitude = latitude,
                longitude = longitude,
            )

        // 저장 직후 또는 재사용 시점 모두 상세 동기화 누락 여부 확인.
        enrichDetailsIfNeeded(place, language)

        return place
    }

    fun getOrCreateFromExternalPlaceId(
        externalPlaceId: String,
        language: String = "ko",
    ): Place {
        // 외부 ID만 받은 resolve 흐름은 먼저 기존 저장분 재사용.
        placeRepository.findByExternalPlaceId(externalPlaceId)?.let { return it }

        // 내부에 없으면 Google summary로 최소 Place 생성에 필요한 좌표/이름 확보.
        val summary = placeSearchGateway.getPlaceSummary(externalPlaceId, language)
            ?: throw BusinessException(
                ErrorCode.PLACE_NOT_FOUND,
                detail = mapOf("externalPlaceId" to externalPlaceId),
            )

        return createPlaceOrFindConcurrent(
            externalPlaceId = summary.externalPlaceId,
            placeName = summary.placeName,
            address = summary.address,
            latitude = BigDecimal.valueOf(summary.latitude),
            longitude = BigDecimal.valueOf(summary.longitude),
            primaryType = summary.primaryType,
            types = summary.types,
        )
    }

    fun mergeWithCanonical(results: List<PlaceSearchGateway.SearchHit>): List<PlaceResult.SearchItem> {
        // 검색 후보에 내부 placeId/detailSummary를 덧입혀 저장 여부를 응답에 반영.
        val existingMap = findExistingPlaces(results)
        return results.map { result -> placeResultAssembler.toSearchItem(result, existingMap[result.externalPlaceId]) }
    }

    fun enrichDetailsIfNeeded(place: Place, language: String = "ko"): Place {
        // detailsSyncedAt은 외부 상세 동기화 완료 여부의 단일 기준.
        if (place.detailsSyncedAt != null) return place
        // Google details 실패는 저장된 기본 장소 정보를 그대로 유지.
        val details = placeSearchGateway.getPlaceDetails(place.externalPlaceId, language) ?: return place
        applyDetails(place, details)
        return place
    }

    private fun createPlaceOrFindConcurrent(
        externalPlaceId: String,
        placeName: String,
        address: String?,
        latitude: BigDecimal,
        longitude: BigDecimal,
        primaryType: String? = null,
        types: List<String> = emptyList(),
    ): Place =
        try {
            // 동시 resolve/save 요청에서도 중복 insert 충돌을 작은 새 트랜잭션에 격리.
            TransactionTemplate(transactionManager).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }.execute {
                val place = Place(
                    externalPlaceId = externalPlaceId,
                    name = placeName,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                )
                if (primaryType != null || types.isNotEmpty()) {
                    // summary 단계에서 받은 Google type은 상세 동기화 전 임시 분류 근거.
                    place.googlePrimaryType = primaryType
                    place.googleTypesJson = types.takeIf { it.isNotEmpty() }?.let(objectMapper::writeValueAsString)
                    place.typeSummarySyncedAt = LocalDateTime.now()
                }
                placeRepository.saveAndFlush(
                    place,
                )
            } ?: throw DataIntegrityViolationException("Failed to save place")
        } catch (ex: DataIntegrityViolationException) {
            // unique key 경합이면 이미 저장된 canonical Place 재조회.
            placeRepository.findByExternalPlaceId(externalPlaceId) ?: throw ex
        }

    private fun applyDetails(place: Place, details: PlaceSearchGateway.DetailsPayload) {
        // Place 본문에는 분류/상태처럼 목록과 상세 양쪽에서 쓰는 요약 필드 반영.
        place.googlePrimaryType = details.primaryType
        place.googleTypesJson = details.types.takeIf { it.isNotEmpty() }?.let(objectMapper::writeValueAsString)
        place.businessStatus = details.businessStatus
        place.utcOffsetMinutes = details.utcOffsetMinutes
        place.typeSummarySyncedAt = LocalDateTime.now()
        place.detailsSyncedAt = LocalDateTime.now()

        // 상세 전용 정보는 별도 snapshot으로 분리해 Place 본문 비대화 방지.
        val snapshot = detailSnapshotRepository.findById(place.id).orElse(
            PlaceDetailSnapshot(place = place)
        )
        snapshot.formattedPhoneNumber = details.formattedPhoneNumber
        snapshot.internationalPhoneNumber = details.internationalPhoneNumber
        snapshot.websiteUri = details.websiteUri
        snapshot.googleMapsUri = details.googleMapsUri
        snapshot.rating = details.rating
        snapshot.userRatingCount = details.userRatingCount
        snapshot.priceLevel = details.priceLevel
        snapshot.regularOpeningHoursJson = details.regularOpeningHoursJson
        snapshot.currentOpeningHoursJson = details.currentOpeningHoursJson
        snapshot.primaryPhotoName = details.primaryPhotoName
        snapshot.editorialSummary = details.editorialSummary
        snapshot.detailsSyncedAt = place.detailsSyncedAt
        snapshot.updatedAt = LocalDateTime.now()
        place.detailSnapshot = detailSnapshotRepository.save(snapshot)

        // 영업시간 period는 Google 최신 상세 기준으로 전체 교체.
        openingPeriodRepository.deleteAllByPlaceId(place.id)
        val periods = details.regularOpeningPeriods.map {
            PlaceRegularOpeningPeriod(
                place = place,
                dayOfWeek = it.dayOfWeek,
                openMinute = it.openMinute,
                closeMinute = it.closeMinute,
                isOvernight = it.isOvernight,
                sequenceNo = it.sequenceNo,
            )
        }
        place.regularOpeningPeriods.clear()
        place.regularOpeningPeriods.addAll(periods)
        openingPeriodRepository.saveAll(periods)
    }
}
