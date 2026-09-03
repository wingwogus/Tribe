package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class OpeningSummaryAssemblerTest {
    private val clock = Clock.fixed(Instant.parse("2026-05-17T03:30:00Z"), ZoneOffset.UTC)
    private val now = LocalDateTime.ofInstant(clock.instant(), clock.zone)
    private val assembler = OpeningSummaryAssembler(clock = clock)

    @Test
    fun `fresh current hours produce current source`() {
        val place = placeFixture(
            currentOpeningHoursJson = """
                {"openNow":true,"nextCloseTime":"2026-05-17T14:00:00+09:00"}
            """.trimIndent(),
            currentOpeningHoursSyncedAt = now,
        )

        val summary = assembler.toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(true, summary.openNow)
        assertEquals(null, summary.nextOpenTime)
        assertEquals("2026-05-17T14:00:00+09:00", summary.nextCloseTime)
        assertEquals(OpeningSummarySource.CURRENT, summary.source)
        assertEquals(false, summary.stale)
    }

    @Test
    fun `fresh current periods compute current source at request time`() {
        val place = placeFixture(
            currentOpeningHoursJson = """
                {
                  "periods": [
                    {
                      "open": {"date": {"year": 2026, "month": 5, "day": 17}, "day": 0, "hour": 10, "minute": 0},
                      "close": {"date": {"year": 2026, "month": 5, "day": 17}, "day": 0, "hour": 14, "minute": 0}
                    }
                  ]
                }
            """.trimIndent(),
            currentOpeningHoursSyncedAt = now,
        )

        val summary = assembler.toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(true, summary.openNow)
        assertEquals(null, summary.nextOpenTime)
        assertEquals("2026-05-17T14:00+09:00", summary.nextCloseTime)
        assertEquals(OpeningSummarySource.CURRENT, summary.source)
    }

    @Test
    fun `fresh current openNow past next close does not return stale current truth`() {
        val place = placeFixture(
            currentOpeningHoursJson = """
                {"openNow":true,"nextCloseTime":"2026-05-17T11:00:00+09:00"}
            """.trimIndent(),
            currentOpeningHoursSyncedAt = now,
        )

        val summary = assembler.toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(null, summary.openNow)
        assertEquals(OpeningSummarySource.UNKNOWN, summary.source)
    }

    @Test
    fun `stale current hours fall back to regular periods`() {
        val place = placeFixture(
            currentOpeningHoursJson = """{"openNow":false}""",
            currentOpeningHoursSyncedAt = now.minusDays(8),
            openingHoursSyncedAt = now,
        )
        place.regularOpeningPeriods.add(period(day = 0, openMinute = 10 * 60, closeMinute = 14 * 60))

        val summary = assembler.toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(true, summary.openNow)
        assertEquals(OpeningSummarySource.REGULAR, summary.source)
        assertEquals("2026-05-17T14:00+09:00", summary.nextCloseTime)
    }

    @Test
    fun `regular periods report next open time when currently closed`() {
        val closedClock = Clock.fixed(Instant.parse("2026-05-16T23:30:00Z"), ZoneOffset.UTC)
        val place = placeFixture(openingHoursSyncedAt = LocalDateTime.ofInstant(closedClock.instant(), closedClock.zone))
        place.regularOpeningPeriods.add(period(day = 0, openMinute = 10 * 60, closeMinute = 14 * 60))

        val summary = OpeningSummaryAssembler(clock = closedClock).toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(false, summary.openNow)
        assertEquals("2026-05-17T10:00+09:00", summary.nextOpenTime)
        assertEquals(null, summary.nextCloseTime)
    }

    @Test
    fun `regular periods account for previous-day overnight windows`() {
        val overnightClock = Clock.fixed(Instant.parse("2026-05-17T16:30:00Z"), ZoneOffset.UTC)
        val place = placeFixture(openingHoursSyncedAt = LocalDateTime.ofInstant(overnightClock.instant(), overnightClock.zone))
        place.regularOpeningPeriods.add(period(day = 0, openMinute = 22 * 60, closeMinute = 2 * 60, isOvernight = true))

        val summary = OpeningSummaryAssembler(clock = overnightClock).toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(true, summary.openNow)
        assertEquals("2026-05-18T02:00+09:00", summary.nextCloseTime)
    }

    @Test
    fun `missing offset or periods returns unknown summary`() {
        val place = placeFixture(utcOffsetMinutes = null, openingHoursSyncedAt = now)

        val summary = assembler.toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(null, summary.openNow)
        assertEquals(OpeningSummarySource.UNKNOWN, summary.source)
        assertEquals(false, summary.stale)
    }

    @Test
    fun `malformed fresh current hours return null instead of failing`() {
        val place = placeFixture(
            currentOpeningHoursJson = "{",
            currentOpeningHoursSyncedAt = now,
            openingHoursSyncedAt = now,
        )
        place.regularOpeningPeriods.add(period(day = 0, openMinute = 10 * 60, closeMinute = 14 * 60))

        assertNull(assembler.toOpeningSummary(place))
    }

    @Test
    fun `parseable unsupported current hours return unknown when no regular periods exist`() {
        val place = placeFixture(
            currentOpeningHoursJson = "{}",
            currentOpeningHoursSyncedAt = now,
        )

        val summary = assembler.toOpeningSummary(place)

        requireNotNull(summary)
        assertEquals(null, summary.openNow)
        assertEquals(OpeningSummarySource.UNKNOWN, summary.source)
        assertEquals(false, summary.stale)
    }

    @Test
    fun `parseable current hours with invalid field types return null`() {
        val place = placeFixture(
            currentOpeningHoursJson = """{"openNow":"true"}""",
            currentOpeningHoursSyncedAt = now,
        )

        assertNull(assembler.toOpeningSummary(place))
    }

    @Test
    fun `parseable current hours with non iso next times return null`() {
        val place = placeFixture(
            currentOpeningHoursJson = """{"openNow":true,"nextCloseTime":"soon"}""",
            currentOpeningHoursSyncedAt = now,
        )

        assertNull(assembler.toOpeningSummary(place))
    }

    @Test
    fun `parseable current hours with invalid periods return null`() {
        val place = placeFixture(
            currentOpeningHoursJson = """
                {"periods":[{"open":{"day":0,"hour":24},"close":{"day":0,"hour":18}}]}
            """.trimIndent(),
            currentOpeningHoursSyncedAt = now,
        )

        assertNull(assembler.toOpeningSummary(place))
    }

    @Test
    fun `regular fallback with invalid utc offset returns null instead of throwing`() {
        val place = placeFixture(utcOffsetMinutes = 2_000, openingHoursSyncedAt = now)
        place.regularOpeningPeriods.add(period(day = 0, openMinute = 10 * 60, closeMinute = 14 * 60))

        assertNull(assembler.toOpeningSummary(place))
    }

    @Test
    fun `regular fallback with invalid period values returns null instead of coercing`() {
        val place = placeFixture(openingHoursSyncedAt = now)
        place.regularOpeningPeriods.add(period(day = 0, openMinute = -1, closeMinute = 14 * 60))

        assertNull(assembler.toOpeningSummary(place))
    }

    private fun placeFixture(
        utcOffsetMinutes: Int? = 540,
        currentOpeningHoursJson: String? = null,
        currentOpeningHoursSyncedAt: LocalDateTime? = null,
        openingHoursSyncedAt: LocalDateTime? = null,
    ): Place {
        val place = Place(
            externalPlaceId = "place-1",
            name = "Cafe",
            address = "Seoul",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
            utcOffsetMinutes = utcOffsetMinutes,
        )
        place.detailSnapshot = PlaceDetailSnapshot(
            place = place,
            currentOpeningHoursJson = currentOpeningHoursJson,
            currentOpeningHoursSyncedAt = currentOpeningHoursSyncedAt,
            openingHoursSyncedAt = openingHoursSyncedAt,
        )
        return place
    }

    private fun period(
        day: Int,
        openMinute: Int,
        closeMinute: Int,
        isOvernight: Boolean = false,
    ): PlaceRegularOpeningPeriod =
        PlaceRegularOpeningPeriod(
            place = placeFixture(),
            dayOfWeek = day,
            openMinute = openMinute,
            closeMinute = closeMinute,
            isOvernight = isOvernight,
        )
}
