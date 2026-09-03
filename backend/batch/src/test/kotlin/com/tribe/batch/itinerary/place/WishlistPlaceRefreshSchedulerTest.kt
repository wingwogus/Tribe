package com.tribe.batch.itinerary.place

import com.tribe.application.itinerary.place.WishlistPlaceRefreshRequest
import com.tribe.application.itinerary.place.WishlistPlaceRefreshSummary
import com.tribe.application.itinerary.place.WishlistPlaceRefreshUseCase
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WishlistPlaceRefreshSchedulerTest {
    private val meterRegistry = SimpleMeterRegistry()

    @Test
    fun `refreshActiveWishlistedPlaces skips service call when disabled and records metric`() {
        val refreshUseCase = RecordingRefreshUseCase()
        val scheduler = WishlistPlaceRefreshScheduler(
            refreshUseCase = refreshUseCase,
            properties = WishlistPlaceRefreshProperties(enabled = false),
            meterRegistry = meterRegistry,
        )

        scheduler.refreshActiveWishlistedPlaces()

        assertThat(refreshUseCase.requests).isEmpty()
        assertThat(
            meterRegistry.find(WishlistPlaceRefreshScheduler.METRIC_RUNS)
                .tag("status", "disabled")
                .counter()
                ?.count(),
        ).isEqualTo(1.0)
    }

    @Test
    fun `refreshActiveWishlistedPlaces passes properties and records summary metrics`() {
        val refreshUseCase = RecordingRefreshUseCase(
            summary = WishlistPlaceRefreshSummary(
                scanned = 5,
                processed = 4,
                skipped = 1,
                succeeded = 3,
                failed = 1,
                retries = 2,
                failureReasons = mapOf("ServerError" to 1),
            ),
        )
        val scheduler = WishlistPlaceRefreshScheduler(
            refreshUseCase = refreshUseCase,
            properties = WishlistPlaceRefreshProperties(
                enabled = true,
                batchSize = 5,
                rateLimitPerMinute = 12,
                maxAttempts = 4,
                retryBackoffMillis = 25,
                language = "ja",
            ),
            meterRegistry = meterRegistry,
        )

        scheduler.refreshActiveWishlistedPlaces()

        assertThat(refreshUseCase.requests).hasSize(1)
        val request = refreshUseCase.requests.single()
        assertThat(request.batchSize).isEqualTo(5)
        assertThat(request.rateLimitPerMinute).isEqualTo(12)
        assertThat(request.maxAttempts).isEqualTo(4)
        assertThat(request.retryBackoffMillis).isEqualTo(25L)
        assertThat(request.language).isEqualTo("ja")
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_RUNS, "status", "partial_failure", 1.0)
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_PLACES, "outcome", "scanned", 5.0)
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_PLACES, "outcome", "processed", 4.0)
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_PLACES, "outcome", "skipped", 1.0)
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_PLACES, "outcome", "succeeded", 3.0)
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_PLACES, "outcome", "failed", 1.0)
        assertThat(meterRegistry.find(WishlistPlaceRefreshScheduler.METRIC_RETRIES).counter()?.count())
            .isEqualTo(2.0)
        assertMetric(
            WishlistPlaceRefreshScheduler.METRIC_FAILURES,
            "reason",
            "ServerError",
            1.0,
        )
    }

    @Test
    fun `refreshActiveWishlistedPlaces records failed run metric when refresh throws`() {
        val refreshUseCase = RecordingRefreshUseCase(
            failure = IllegalStateException("scheduler failure"),
        )
        val scheduler = WishlistPlaceRefreshScheduler(
            refreshUseCase = refreshUseCase,
            properties = WishlistPlaceRefreshProperties(enabled = true),
            meterRegistry = meterRegistry,
        )

        scheduler.refreshActiveWishlistedPlaces()

        assertThat(refreshUseCase.requests).hasSize(1)
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_RUNS, "status", "failed", 1.0)
        assertMetric(WishlistPlaceRefreshScheduler.METRIC_FAILURES, "reason", "IllegalStateException", 1.0)
    }

    private fun assertMetric(
        name: String,
        tagKey: String,
        tagValue: String,
        expected: Double,
    ) {
        assertThat(
            meterRegistry.find(name)
                .tag(tagKey, tagValue)
                .counter()
                ?.count(),
        ).isEqualTo(expected)
    }

    private class RecordingRefreshUseCase(
        private val summary: WishlistPlaceRefreshSummary = WishlistPlaceRefreshSummary(
            scanned = 0,
            processed = 0,
            skipped = 0,
            succeeded = 0,
            failed = 0,
            retries = 0,
        ),
        private val failure: RuntimeException? = null,
    ) : WishlistPlaceRefreshUseCase {
        val requests: MutableList<WishlistPlaceRefreshRequest> = mutableListOf()

        override fun refreshActiveWishlistPlaces(request: WishlistPlaceRefreshRequest): WishlistPlaceRefreshSummary {
            requests.add(request)
            failure?.let { throw it }
            return summary
        }
    }
}
