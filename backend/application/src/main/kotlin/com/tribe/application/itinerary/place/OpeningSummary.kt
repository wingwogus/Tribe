package com.tribe.application.itinerary.place

import java.time.LocalDateTime

data class OpeningSummary(
    val openNow: Boolean?,
    val nextOpenTime: String?,
    val nextCloseTime: String?,
    val source: OpeningSummarySource,
    val timezoneOffsetMinutes: Int?,
    val syncedAt: LocalDateTime?,
    val stale: Boolean,
)

enum class OpeningSummarySource {
    CURRENT,
    REGULAR,
    UNKNOWN,
}
