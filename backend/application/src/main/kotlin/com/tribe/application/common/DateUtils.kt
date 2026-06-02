package com.tribe.application.common

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 공통 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
object DateUtils {
    private val apiZone: ZoneId = ZoneId.of("Asia/Seoul")
    private val apiFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun getTodayForApi(): String {
        var currentDate = LocalDate.now(apiZone)

        if (currentDate.dayOfWeek == DayOfWeek.SATURDAY) {
            currentDate = currentDate.minusDays(1)
        }
        if (currentDate.dayOfWeek == DayOfWeek.SUNDAY) {
            currentDate = currentDate.minusDays(2)
        }
        return currentDate.format(apiFormatter)
    }

    fun parseApiDate(dateString: String): LocalDate =
        LocalDate.parse(dateString, apiFormatter)
}
