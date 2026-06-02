package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 장소 영업시간 판정기.
 *
 * 일정 방문 날짜/시간을 Google opening period와 비교해 표시 상태로 축소.
 */
@Component
class OpeningHoursEvaluator {
    fun evaluate(item: ItineraryItem, tripStartDate: LocalDate): String? {
        // 장소 없는 일정은 영업시간 판단 대상 아님.
        val place = item.place ?: return null
        if (place.businessStatus == "CLOSED_TEMPORARILY") return "TEMPORARILY_CLOSED"
        // visitDay는 여행 시작일 기준 1일부터 시작.
        val visitDate = tripStartDate.plusDays((item.visitDay - 1).toLong())
        val dayOfWeek = toGoogleDayOfWeek(visitDate)
        val previousDay = (dayOfWeek + 6) % 7
        if (place.regularOpeningPeriods.isEmpty()) return null

        // 심야 영업은 전날 open period가 다음날 closeMinute까지 이어지는 구조.
        val todayPeriods = place.regularOpeningPeriods.filter { it.dayOfWeek == dayOfWeek }
        val previousDayOvernightPeriods = place.regularOpeningPeriods.filter { it.dayOfWeek == previousDay && it.isOvernight }

        // 방문 시간이 없으면 휴무 가능성만 보수적으로 표시.
        val visitTime = item.time ?: return if (todayPeriods.isEmpty()) "CLOSED_DAY_POSSIBLE" else null

        val minuteOfDay = visitTime.hour * 60 + visitTime.minute

        // 오늘 period와 전날 overnight period를 함께 확인.
        if (todayPeriods.isEmpty() && previousDayOvernightPeriods.isEmpty()) return "CLOSED_DAY"
        if (todayPeriods.any { isWithinPeriod(it, minuteOfDay) }) return "OPEN"
        if (previousDayOvernightPeriods.any { minuteOfDay < it.closeMinute }) return "OPEN"
        return "OUTSIDE_BUSINESS_HOURS"
    }

    private fun toGoogleDayOfWeek(value: LocalDate): Int = value.dayOfWeek.value % 7

    private fun isWithinPeriod(period: PlaceRegularOpeningPeriod, minuteOfDay: Int): Boolean {
        return if (period.isOvernight) {
            minuteOfDay >= period.openMinute
        } else {
            minuteOfDay in period.openMinute until period.closeMinute
        }
    }
}
