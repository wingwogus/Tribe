package com.tribe.batch.itinerary.place

import com.tribe.application.itinerary.place.WishlistPlaceRefreshRequest
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tribe.itinerary.place-refresh")
data class WishlistPlaceRefreshProperties(
    var enabled: Boolean = true,
    var batchSize: Int = 100,
    var rateLimitPerMinute: Int = 60,
    var maxAttempts: Int = 3,
    var retryBackoffMillis: Long = 500,
    var language: String = "ko",
    var detailsFreshnessDays: Long = 30,
    var regularHoursFreshnessDays: Long = 30,
    var currentHoursFreshnessDays: Long = 7,
) {
    fun toRequest(): WishlistPlaceRefreshRequest =
        WishlistPlaceRefreshRequest(
            batchSize = batchSize,
            rateLimitPerMinute = rateLimitPerMinute,
            maxAttempts = maxAttempts,
            retryBackoffMillis = retryBackoffMillis,
            language = language,
            detailsFreshnessDays = detailsFreshnessDays,
            regularHoursFreshnessDays = regularHoursFreshnessDays,
            currentHoursFreshnessDays = currentHoursFreshnessDays,
        )
}
