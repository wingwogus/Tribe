package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

data class WishlistPlaceRefreshRequest(
    val batchSize: Int = 100,
    val rateLimitPerMinute: Int = 60,
    val maxAttempts: Int = 3,
    val retryBackoffMillis: Long = 500,
    val language: String = "ko",
    val detailsFreshnessDays: Long = 30,
    val regularHoursFreshnessDays: Long = 30,
    val currentHoursFreshnessDays: Long = 7,
) {
    fun normalized(): WishlistPlaceRefreshRequest =
        copy(
            batchSize = batchSize.coerceAtLeast(1),
            rateLimitPerMinute = rateLimitPerMinute.coerceAtLeast(1),
            maxAttempts = maxAttempts.coerceAtLeast(1),
            retryBackoffMillis = retryBackoffMillis.coerceAtLeast(0),
            language = language.trim().takeIf { it.isNotEmpty() } ?: "ko",
            detailsFreshnessDays = detailsFreshnessDays.coerceAtLeast(0),
            regularHoursFreshnessDays = regularHoursFreshnessDays.coerceAtLeast(0),
            currentHoursFreshnessDays = currentHoursFreshnessDays.coerceAtLeast(0),
        )
}

data class WishlistPlaceRefreshSummary(
    val scanned: Int,
    val processed: Int,
    val skipped: Int,
    val succeeded: Int,
    val failed: Int,
    val retries: Int,
    val failureReasons: Map<String, Int> = emptyMap(),
)

fun interface WishlistPlaceRefreshUseCase {
    fun refreshActiveWishlistPlaces(request: WishlistPlaceRefreshRequest): WishlistPlaceRefreshSummary
}

fun interface WishlistPlaceRefreshSleeper {
    fun sleep(millis: Long)
}

@Component
class ThreadWishlistPlaceRefreshSleeper : WishlistPlaceRefreshSleeper {
    override fun sleep(millis: Long) {
        if (millis > 0) {
            Thread.sleep(millis)
        }
    }
}

@Service
class WishlistPlaceRefreshService(
    private val placeRepository: PlaceRepository,
    private val placeCatalogService: PlaceCatalogService,
    private val sleeper: WishlistPlaceRefreshSleeper,
) : WishlistPlaceRefreshUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun refreshActiveWishlistPlaces(request: WishlistPlaceRefreshRequest): WishlistPlaceRefreshSummary {
        val normalizedRequest = request.normalized()
        val now = LocalDateTime.now()
        val detailsCutoff = now.minusDays(normalizedRequest.detailsFreshnessDays)
        val regularHoursCutoff = now.minusDays(normalizedRequest.regularHoursFreshnessDays)
        val currentHoursCutoff = now.minusDays(normalizedRequest.currentHoursFreshnessDays)
        val candidates = placeRepository.findActiveWishlistedPlacesForRefresh(
            detailsCutoff,
            regularHoursCutoff,
            currentHoursCutoff,
            PageRequest.of(0, normalizedRequest.batchSize),
        )
        val accumulator = RefreshAccumulator()
        var googleCallCount = 0

        candidates.forEach { place ->
            if (!place.needsRefresh(now, normalizedRequest)) {
                accumulator.skipped += 1
                return@forEach
            }

            accumulator.processed += 1
            val result = refreshWithRetry(place, normalizedRequest) {
                throttleGoogleCall(googleCallCount, normalizedRequest.rateLimitPerMinute)
                googleCallCount += 1
            }
            accumulator.retries += result.retries
            if (result.succeeded) {
                accumulator.succeeded += 1
            } else {
                accumulator.failed += 1
                accumulator.addFailure(result.failureReason ?: "unknown")
            }
        }

        val summary = accumulator.toSummary(candidates.size)
        log.info(
            "Active wishlist place refresh completed: scanned={}, processed={}, skipped={}, succeeded={}, failed={}, retries={}",
            summary.scanned,
            summary.processed,
            summary.skipped,
            summary.succeeded,
            summary.failed,
            summary.retries,
        )
        return summary
    }

    private fun refreshWithRetry(
        place: Place,
        request: WishlistPlaceRefreshRequest,
        beforeGoogleCall: () -> Unit,
    ): RefreshResult {
        var attempt = 1
        var retries = 0
        while (true) {
            try {
                beforeGoogleCall()
                val refreshed = placeCatalogService.refreshDetailsById(place.id, request.language)
                if (refreshed) {
                    return RefreshResult(succeeded = true, retries = retries)
                }
                log.warn(
                    "Active wishlist place refresh returned no details: placeId={}, externalPlaceId={}, attempt={}",
                    place.id,
                    place.externalPlaceId,
                    attempt,
                )
                return RefreshResult(succeeded = false, retries = retries, failureReason = "empty_details")
            } catch (ex: Exception) {
                val reason = ex::class.simpleName ?: "exception"
                if (!ex.isRetryableRefreshFailure() || attempt >= request.maxAttempts) {
                    log.warn(
                        "Active wishlist place refresh failed: placeId={}, externalPlaceId={}, attempt={}, reason={}",
                        place.id,
                        place.externalPlaceId,
                        attempt,
                        reason,
                        ex,
                    )
                    return RefreshResult(succeeded = false, retries = retries, failureReason = reason)
                }

                retries += 1
                log.warn(
                    "Retrying active wishlist place refresh: placeId={}, externalPlaceId={}, attempt={}, reason={}",
                    place.id,
                    place.externalPlaceId,
                    attempt,
                    reason,
                    ex,
                )
                sleeper.sleep(request.retryBackoffMillis)
                attempt += 1
            }
        }
    }

    private fun Exception.isRetryableRefreshFailure(): Boolean =
        when (this) {
            is WebClientResponseException -> statusCode.value() == 429 || statusCode.is5xxServerError
            is WebClientRequestException -> true
            else -> false
        }

    private fun throttleGoogleCall(
        callIndex: Int,
        rateLimitPerMinute: Int,
    ) {
        if (callIndex == 0) return
        sleeper.sleep(60_000L / rateLimitPerMinute)
    }

    private fun Place.needsRefresh(
        now: LocalDateTime,
        request: WishlistPlaceRefreshRequest,
    ): Boolean {
        val snapshot = detailSnapshot
        val detailsSyncedAt = detailsSyncedAt ?: snapshot?.detailsSyncedAt
        return detailsSyncedAt.isMissingOrOlderThan(now.minusDays(request.detailsFreshnessDays)) ||
            snapshot == null ||
            snapshot.openingHoursSyncedAt.isMissingOrOlderThan(now.minusDays(request.regularHoursFreshnessDays)) ||
            snapshot.currentOpeningHoursSyncedAt.isMissingOrOlderThan(now.minusDays(request.currentHoursFreshnessDays))
    }

    private fun LocalDateTime?.isMissingOrOlderThan(cutoff: LocalDateTime): Boolean =
        this == null || isBefore(cutoff)

    private data class RefreshResult(
        val succeeded: Boolean,
        val retries: Int,
        val failureReason: String? = null,
    )

    private class RefreshAccumulator {
        var processed: Int = 0
        var skipped: Int = 0
        var succeeded: Int = 0
        var failed: Int = 0
        var retries: Int = 0
        private val failureReasons: MutableMap<String, Int> = linkedMapOf()

        fun addFailure(reason: String) {
            failureReasons[reason] = (failureReasons[reason] ?: 0) + 1
        }

        fun toSummary(scanned: Int): WishlistPlaceRefreshSummary =
            WishlistPlaceRefreshSummary(
                scanned = scanned,
                processed = processed,
                skipped = skipped,
                succeeded = succeeded,
                failed = failed,
                retries = retries,
                failureReasons = failureReasons.toMap(),
            )
    }
}
