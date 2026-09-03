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
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 저장된 장소 catalog use case.
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
    private val log = LoggerFactory.getLogger(javaClass)

    fun findExistingPlaces(results: List<PlaceSearchGateway.SearchHit>): Map<String, Place> {
        // 외부 후보 목록에서 이미 저장된 Place만 한 번에 조회.
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
        // 흐름: 기존 Place 조회 -> 없으면 새 Place 저장 -> 상세 정보 보강.
        val place = placeRepository.findByExternalPlaceId(externalPlaceId)
            ?: createPlaceOrFindConcurrent(
                externalPlaceId = externalPlaceId,
                placeName = placeName,
                address = address,
                latitude = latitude,
                longitude = longitude,
            ).let(::findManagedPlace)

        return try {
            enrichDetailsIfNeeded(place, language)
        } catch (ex: BusinessException) {
            if (ex.errorCode != ErrorCode.EXTERNAL_API_ERROR) throw ex
            log.warn(
                "Skipping Google place detail enrichment: externalPlaceId={}, placeId={}",
                place.externalPlaceId,
                place.id,
            )
            place
        }
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

    fun mergeWithSavedPlaces(results: List<PlaceSearchGateway.SearchHit>): List<PlaceResult.SearchItem> {
        // 검색 후보에 내부 placeId/detailSummary를 덧입혀 저장 여부를 응답에 반영.
        val existingMap = findExistingPlaces(results)
        return results.map { result -> placeResultAssembler.toSearchItem(result, existingMap[result.externalPlaceId]) }
    }

    fun mergeNearbyWithSavedPlaces(results: List<PlaceSearchGateway.SearchHit>): List<PlaceResult.SearchItem> {
        // 주변 후보는 내부 placeId/type만 얇게 병합하고 상세/사진/영업시간 조립은 건너뛴다.
        val existingMap = findExistingPlaces(results)
        return results.map { result -> placeResultAssembler.toNearbySearchItem(result, existingMap[result.externalPlaceId]) }
    }

    fun enrichDetailsIfNeeded(place: Place, language: String = "ko"): Place {
        if (!needsDetailEnrichment(place)) return place
        refreshDetails(place, language)
        return place
    }

    fun refreshDetailsById(placeId: Long, language: String = "ko"): Boolean {
        val place = placeRepository.findById(placeId).orElse(null) ?: return false
        return refreshDetails(place, language)
    }

    fun refreshDetails(place: Place, language: String = "ko"): Boolean {
        val details = placeSearchGateway.getPlaceDetails(place.externalPlaceId, language) ?: return false
        applyDetails(place, details)
        return true
    }

    private fun needsDetailEnrichment(place: Place): Boolean {
        val snapshot = place.detailSnapshot
        return place.detailsSyncedAt == null ||
            snapshot == null ||
            snapshot.openingHoursSyncedAt == null ||
            snapshot.currentOpeningHoursSyncedAt == null
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
            // unique key 경합이면 이미 저장된 Place 재조회.
            placeRepository.findByExternalPlaceId(externalPlaceId) ?: throw ex
        }

    private fun findManagedPlace(place: Place): Place =
        if (place.id == 0L) {
            place
        } else {
            placeRepository.getReferenceById(place.id)
        }

    private fun applyDetails(place: Place, details: PlaceSearchGateway.DetailsPayload) {
        val syncedAt = LocalDateTime.now()
        // Place 본문에는 분류/상태처럼 목록과 상세 양쪽에서 쓰는 요약 필드 반영.
        place.googlePrimaryType = details.primaryType
        place.googleTypesJson = details.types.takeIf { it.isNotEmpty() }?.let(objectMapper::writeValueAsString)
        place.businessStatus = details.businessStatus
        place.utcOffsetMinutes = details.utcOffsetMinutes
        place.typeSummarySyncedAt = syncedAt
        place.detailsSyncedAt = syncedAt

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
        snapshot.openingHoursSyncedAt = syncedAt
        snapshot.currentOpeningHoursSyncedAt = syncedAt
        snapshot.primaryPhotoName = details.primaryPhotoName
        snapshot.editorialSummary = details.editorialSummary
        snapshot.detailsSyncedAt = syncedAt
        snapshot.updatedAt = syncedAt
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
