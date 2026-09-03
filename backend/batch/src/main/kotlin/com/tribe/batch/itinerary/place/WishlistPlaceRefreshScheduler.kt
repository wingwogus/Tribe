package com.tribe.batch.itinerary.place

import com.tribe.application.itinerary.place.WishlistPlaceRefreshSummary
import com.tribe.application.itinerary.place.WishlistPlaceRefreshUseCase
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties(WishlistPlaceRefreshProperties::class)
class WishlistPlaceRefreshScheduler(
    private val refreshUseCase: WishlistPlaceRefreshUseCase,
    private val properties: WishlistPlaceRefreshProperties,
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        cron = "\${tribe.itinerary.place-refresh.cron:0 0 4 * * MON}",
        zone = "\${tribe.itinerary.place-refresh.zone:Asia/Seoul}",
    )
    fun refreshActiveWishlistedPlaces() {
        if (!properties.enabled) {
            log.info("Active wishlist place refresh skipped because scheduler is disabled")
            meterRegistry.counter(METRIC_RUNS, "status", "disabled").increment()
            return
        }

        log.info(
            "Starting active wishlist place refresh: batchSize={}, rateLimitPerMinute={}, maxAttempts={}",
            properties.batchSize,
            properties.rateLimitPerMinute,
            properties.maxAttempts,
        )
        try {
            val summary = refreshUseCase.refreshActiveWishlistPlaces(properties.toRequest())
            recordMetrics(summary)
            log.info(
                "Finished active wishlist place refresh: scanned={}, processed={}, skipped={}, succeeded={}, failed={}, retries={}",
                summary.scanned,
                summary.processed,
                summary.skipped,
                summary.succeeded,
                summary.failed,
                summary.retries,
            )
        } catch (ex: Exception) {
            val reason = ex::class.simpleName ?: "exception"
            meterRegistry.counter(METRIC_RUNS, "status", "failed").increment()
            meterRegistry.counter(METRIC_FAILURES, "reason", reason).increment()
            log.error("Active wishlist place refresh failed before summary: reason={}", reason, ex)
        }
    }

    private fun recordMetrics(summary: WishlistPlaceRefreshSummary) {
        meterRegistry.counter(METRIC_RUNS, "status", if (summary.failed > 0) "partial_failure" else "success").increment()
        meterRegistry.counter(METRIC_PLACES, "outcome", "scanned").increment(summary.scanned.toDouble())
        meterRegistry.counter(METRIC_PLACES, "outcome", "processed").increment(summary.processed.toDouble())
        meterRegistry.counter(METRIC_PLACES, "outcome", "skipped").increment(summary.skipped.toDouble())
        meterRegistry.counter(METRIC_PLACES, "outcome", "succeeded").increment(summary.succeeded.toDouble())
        meterRegistry.counter(METRIC_PLACES, "outcome", "failed").increment(summary.failed.toDouble())
        meterRegistry.counter(METRIC_RETRIES).increment(summary.retries.toDouble())
        summary.failureReasons.forEach { (reason, count) ->
            meterRegistry.counter(METRIC_FAILURES, "reason", reason).increment(count.toDouble())
        }
    }

    companion object {
        const val METRIC_RUNS = "tribe.wishlist_place_refresh.runs"
        const val METRIC_PLACES = "tribe.wishlist_place_refresh.places"
        const val METRIC_RETRIES = "tribe.wishlist_place_refresh.retries"
        const val METRIC_FAILURES = "tribe.wishlist_place_refresh.failures"
    }
}
