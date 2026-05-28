package com.tribe.application.itinerary.place

import com.fasterxml.jackson.databind.ObjectMapper
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
            ).let(::findManagedPlace)

        return enrichDetailsIfNeeded(place, language)
    }

    fun mergeWithCanonical(results: List<PlaceSearchGateway.SearchHit>): List<PlaceResult.SearchItem> {
        val existingMap = findExistingPlaces(results)
        return results.map { result -> placeResultAssembler.toSearchItem(result, existingMap[result.externalPlaceId]) }
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
    ): Place =
        try {
            TransactionTemplate(transactionManager).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }.execute {
                placeRepository.saveAndFlush(
                    Place(
                        externalPlaceId = externalPlaceId,
                        name = placeName,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                    ),
                )
            } ?: throw DataIntegrityViolationException("Failed to save place")
        } catch (ex: DataIntegrityViolationException) {
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
        place.googlePrimaryType = details.primaryType
        place.googleTypesJson = details.types.takeIf { it.isNotEmpty() }?.let(objectMapper::writeValueAsString)
        place.businessStatus = details.businessStatus
        place.utcOffsetMinutes = details.utcOffsetMinutes
        place.typeSummarySyncedAt = syncedAt
        place.detailsSyncedAt = syncedAt

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
