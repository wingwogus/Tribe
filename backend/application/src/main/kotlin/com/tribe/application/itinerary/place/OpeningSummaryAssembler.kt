package com.tribe.application.itinerary.place

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class OpeningSummaryAssembler(
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val currentHoursFreshnessDays: Long = 7,
    private val regularHoursFreshnessDays: Long = 30,
) {
    fun toOpeningSummary(place: Place?): OpeningSummary? {
        if (place == null) return null
        val snapshot = place.detailSnapshot

        val currentSyncedAt = snapshot?.currentOpeningHoursSyncedAt
        val currentJson = snapshot?.currentOpeningHoursJson
        if (currentSyncedAt != null && currentJson != null && !isStale(currentSyncedAt, currentHoursFreshnessDays)) {
            return parseCurrentSummary(place, currentJson, currentSyncedAt)
        }

        return regularSummary(place)
    }

    private fun parseCurrentSummary(
        place: Place,
        currentJson: String,
        syncedAt: LocalDateTime,
    ): OpeningSummary? {
        val node = runCatching { objectMapper.readTree(currentJson) }.getOrNull() ?: return null
        if (!node.isObject) return null
        val offset = place.utcOffsetMinutes?.let {
            runCatching { ZoneOffset.ofTotalSeconds(Math.multiplyExact(it, 60)) }.getOrNull() ?: return null
        }
        val now = offset?.let { OffsetDateTime.now(clock).withOffsetSameInstant(it) }
        val currentWindows = runCatching { currentWindowsFromJson(node, now, offset) }.getOrNull() ?: return null
        if (currentWindows.isNotEmpty() && now != null) {
            return summaryFromWindows(
                windows = currentWindows,
                now = now,
                source = OpeningSummarySource.CURRENT,
                timezoneOffsetMinutes = place.utcOffsetMinutes,
                syncedAt = syncedAt,
                stale = false,
            )
        }

        val currentFields = runCatching {
            CurrentOpeningFields(
                openNow = node.booleanOrNull("openNow"),
                nextOpenTime = node.isoTimeOrNull("nextOpenTime"),
                nextCloseTime = node.isoTimeOrNull("nextCloseTime"),
            )
        }.getOrNull() ?: return null

        val boundarySummary = now?.let { currentFields.toBoundarySummary(place, syncedAt, it) }
        if (boundarySummary != null) {
            return boundarySummary
        }

        return fallbackFromCurrent(place, syncedAt)
    }

    private fun regularSummary(place: Place): OpeningSummary? {
        val snapshot = place.detailSnapshot
        val syncedAt = snapshot?.openingHoursSyncedAt ?: snapshot?.detailsSyncedAt ?: place.detailsSyncedAt
        val stale = syncedAt == null || isStale(syncedAt, regularHoursFreshnessDays)
        val timezoneOffsetMinutes = place.utcOffsetMinutes
        val rawPeriods = place.regularOpeningPeriods
        if (rawPeriods.any { !it.hasSupportedOpeningWindow() }) {
            return null
        }

        val periods = rawPeriods
            .sortedWith(compareBy<PlaceRegularOpeningPeriod> { it.dayOfWeek }.thenBy { it.sequenceNo })

        if (timezoneOffsetMinutes == null || periods.isEmpty()) {
            return unknownSummary(place, syncedAt, stale)
        }

        val offset = runCatching {
            ZoneOffset.ofTotalSeconds(Math.multiplyExact(timezoneOffsetMinutes, 60))
        }.getOrNull() ?: return null
        val now = OffsetDateTime.now(clock).withOffsetSameInstant(offset)
        val windows = windowsAround(now.toLocalDate(), offset, periods)
        return summaryFromWindows(
            windows = windows,
            now = now,
            source = OpeningSummarySource.REGULAR,
            timezoneOffsetMinutes = timezoneOffsetMinutes,
            syncedAt = syncedAt,
            stale = stale,
        )
    }

    private fun summaryFromWindows(
        windows: List<OpeningWindow>,
        now: OffsetDateTime,
        source: OpeningSummarySource,
        timezoneOffsetMinutes: Int?,
        syncedAt: LocalDateTime?,
        stale: Boolean,
    ): OpeningSummary {
        val activeWindow = windows
            .filter { !now.isBefore(it.openAt) && now.isBefore(it.closeAt) }
            .minByOrNull { it.closeAt }

        return if (activeWindow != null) {
            OpeningSummary(
                openNow = true,
                nextOpenTime = null,
                nextCloseTime = activeWindow.closeAt.toString(),
                source = source,
                timezoneOffsetMinutes = timezoneOffsetMinutes,
                syncedAt = syncedAt,
                stale = stale,
            )
        } else {
            OpeningSummary(
                openNow = false,
                nextOpenTime = windows.firstOrNull { it.openAt.isAfter(now) }?.openAt?.toString(),
                nextCloseTime = null,
                source = source,
                timezoneOffsetMinutes = timezoneOffsetMinutes,
                syncedAt = syncedAt,
                stale = stale,
            )
        }
    }

    private fun fallbackFromCurrent(
        place: Place,
        syncedAt: LocalDateTime,
    ): OpeningSummary? =
        if (place.utcOffsetMinutes != null && place.regularOpeningPeriods.isNotEmpty()) {
            regularSummary(place)
        } else {
            unknownSummary(place, syncedAt, stale = false)
        }

    private fun currentWindowsFromJson(
        node: JsonNode,
        now: OffsetDateTime?,
        offset: ZoneOffset?,
    ): List<OpeningWindow> {
        val periodsNode = node.get("periods") ?: return emptyList()
        if (periodsNode.isNull) return emptyList()
        require(periodsNode.isArray) { "periods must be an array" }
        if (now == null || offset == null) return emptyList()

        return periodsNode.map { periodNode ->
            val open = periodNode.objectField("open")
            val close = periodNode.objectField("close")
            val openDay = open.intValueOrNull("day", 0..6)
            val closeDay = close.intValueOrNull("day", 0..6, defaultValue = openDay)
            val openMinute = open.timeMinuteOfDay()
            val closeMinute = close.timeMinuteOfDay()
            val openDate = open.dateOrNull("date") ?: dateForGoogleDay(now.toLocalDate(), openDay)
            val closeDate = close.dateOrNull("date")
                ?: if (closeDay != openDay || closeMinute <= openMinute) {
                    openDate.plusDays(1)
                } else {
                    openDate
                }
            OpeningWindow(
                openAt = atMinute(openDate, openMinute, offset),
                closeAt = atMinute(closeDate, closeMinute, offset),
            )
        }.sortedBy { it.openAt }
    }

    private fun dateForGoogleDay(
        localDate: LocalDate,
        googleDay: Int,
    ): LocalDate =
        (-1L..7L)
            .map { localDate.plusDays(it) }
            .first { it.dayOfWeek.value % 7 == googleDay }

    private fun windowsAround(
        localDate: LocalDate,
        offset: ZoneOffset,
        periods: List<PlaceRegularOpeningPeriod>,
    ): List<OpeningWindow> =
        (-1L..7L).flatMap { dayOffset ->
            val date = localDate.plusDays(dayOffset)
            val googleDay = date.dayOfWeek.value % 7
            periods.filter { it.dayOfWeek == googleDay }.map { period ->
                val openAt = atMinute(date, period.openMinute, offset)
                val closeDate = if (period.isOvernight || period.closeMinute <= period.openMinute) {
                    date.plusDays(1)
                } else {
                    date
                }
                val closeAt = atMinute(closeDate, period.closeMinute, offset)
                OpeningWindow(openAt = openAt, closeAt = closeAt)
            }
        }.sortedBy { it.openAt }

    private fun atMinute(
        date: LocalDate,
        minuteOfDay: Int,
        offset: ZoneOffset,
    ): OffsetDateTime {
        return OffsetDateTime.of(
            date,
            LocalTime.of(minuteOfDay / 60, minuteOfDay % 60),
            offset,
        )
    }

    private fun PlaceRegularOpeningPeriod.hasSupportedOpeningWindow(): Boolean =
        dayOfWeek in 0..6 &&
            openMinute in 0 until MINUTES_PER_DAY &&
            closeMinute in 0 until MINUTES_PER_DAY

    private fun isStale(syncedAt: LocalDateTime, freshnessDays: Long): Boolean =
        syncedAt.isBefore(LocalDateTime.now(clock).minusDays(freshnessDays))

    private fun unknownSummary(
        place: Place,
        syncedAt: LocalDateTime?,
        stale: Boolean,
    ): OpeningSummary =
        OpeningSummary(
            openNow = null,
            nextOpenTime = null,
            nextCloseTime = null,
            source = OpeningSummarySource.UNKNOWN,
            timezoneOffsetMinutes = place.utcOffsetMinutes,
            syncedAt = syncedAt,
            stale = stale,
        )

    private fun JsonNode.booleanOrNull(fieldName: String): Boolean? {
        val value = get(fieldName) ?: return null
        if (value.isNull) return null
        require(value.isBoolean) { "$fieldName must be boolean" }
        return value.asBoolean()
    }

    private fun JsonNode.isoTimeOrNull(fieldName: String): BoundaryTime? {
        val value = get(fieldName) ?: return null
        if (value.isNull) return null
        require(value.isTextual) { "$fieldName must be an ISO timestamp string" }
        val raw = value.asText().takeIf { it.isNotBlank() } ?: return null
        return BoundaryTime(raw = raw, value = OffsetDateTime.parse(raw))
    }

    private fun CurrentOpeningFields.toBoundarySummary(
        place: Place,
        syncedAt: LocalDateTime,
        now: OffsetDateTime,
    ): OpeningSummary? =
        when {
            openNow == true && nextCloseTime != null && now.toInstant().isBefore(nextCloseTime.value.toInstant()) ->
                OpeningSummary(
                    openNow = true,
                    nextOpenTime = null,
                    nextCloseTime = nextCloseTime.raw,
                    source = OpeningSummarySource.CURRENT,
                    timezoneOffsetMinutes = place.utcOffsetMinutes,
                    syncedAt = syncedAt,
                    stale = false,
                )

            openNow == false && nextOpenTime != null && now.toInstant().isBefore(nextOpenTime.value.toInstant()) ->
                OpeningSummary(
                    openNow = false,
                    nextOpenTime = nextOpenTime.raw,
                    nextCloseTime = null,
                    source = OpeningSummarySource.CURRENT,
                    timezoneOffsetMinutes = place.utcOffsetMinutes,
                    syncedAt = syncedAt,
                    stale = false,
                )

            else -> null
        }

    private fun JsonNode.objectField(fieldName: String): JsonNode {
        val value = get(fieldName)
        require(value != null && value.isObject) { "$fieldName must be an object" }
        return value
    }

    private fun JsonNode.timeMinuteOfDay(): Int {
        val hour = intValueOrNull("hour", 0..23, defaultValue = 0)
        val minute = intValueOrNull("minute", 0..59, defaultValue = 0)
        return hour * 60 + minute
    }

    private fun JsonNode.dateOrNull(fieldName: String): LocalDate? {
        val value = get(fieldName) ?: return null
        if (value.isNull) return null
        require(value.isObject) { "$fieldName must be an object" }
        return LocalDate.of(
            value.intValueOrNull("year", 1..9999),
            value.intValueOrNull("month", 1..12),
            value.intValueOrNull("day", 1..31),
        )
    }

    private fun JsonNode.intValueOrNull(
        fieldName: String,
        range: IntRange,
        defaultValue: Int? = null,
    ): Int {
        val value = get(fieldName) ?: return defaultValue
            ?: throw IllegalArgumentException("$fieldName is required")
        require(value.isIntegralNumber && value.canConvertToInt()) { "$fieldName must be an integer" }
        return value.intValue().also {
            require(it in range) { "$fieldName is out of supported range" }
        }
    }

    private data class CurrentOpeningFields(
        val openNow: Boolean?,
        val nextOpenTime: BoundaryTime?,
        val nextCloseTime: BoundaryTime?,
    )

    private data class BoundaryTime(
        val raw: String,
        val value: OffsetDateTime,
    )

    private data class OpeningWindow(
        val openAt: OffsetDateTime,
        val closeAt: OffsetDateTime,
    )

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
