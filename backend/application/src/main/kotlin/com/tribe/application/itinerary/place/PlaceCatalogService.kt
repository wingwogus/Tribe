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
        val place = placeRepository.findByExternalPlaceId(externalPlaceId)
            ?: createPlaceOrFindConcurrent(
                externalPlaceId = externalPlaceId,
                placeName = placeName,
                address = address,
                latitude = latitude,
                longitude = longitude,
            )

        enrichDetailsIfNeeded(place, language)

        return place
    }

    fun getOrCreateFromExternalPlaceId(
        externalPlaceId: String,
        language: String = "ko",
    ): Place {
        placeRepository.findByExternalPlaceId(externalPlaceId)?.let { return it }

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
        val existingMap = findExistingPlaces(results)
        return results.map { result -> placeResultAssembler.toSearchItem(result, existingMap[result.externalPlaceId]) }
    }

    fun enrichDetailsIfNeeded(place: Place, language: String = "ko"): Place {
        if (place.detailsSyncedAt != null) return place
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
                    place.googlePrimaryType = primaryType
                    place.googleTypesJson = types.takeIf { it.isNotEmpty() }?.let(objectMapper::writeValueAsString)
                    place.typeSummarySyncedAt = LocalDateTime.now()
                }
                placeRepository.saveAndFlush(
                    place,
                )
            } ?: throw DataIntegrityViolationException("Failed to save place")
        } catch (ex: DataIntegrityViolationException) {
            placeRepository.findByExternalPlaceId(externalPlaceId) ?: throw ex
        }

    private fun applyDetails(place: Place, details: PlaceSearchGateway.DetailsPayload) {
        place.googlePrimaryType = details.primaryType
        place.googleTypesJson = details.types.takeIf { it.isNotEmpty() }?.let(objectMapper::writeValueAsString)
        place.businessStatus = details.businessStatus
        place.utcOffsetMinutes = details.utcOffsetMinutes
        place.typeSummarySyncedAt = LocalDateTime.now()
        place.detailsSyncedAt = LocalDateTime.now()

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
