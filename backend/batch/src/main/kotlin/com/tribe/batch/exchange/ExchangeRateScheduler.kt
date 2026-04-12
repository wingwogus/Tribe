package com.tribe.batch.exchange

import com.tribe.application.common.DateUtils
import com.tribe.application.exchange.ExchangeRateService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExchangeRateScheduler(
    private val exchangeRateService: ExchangeRateService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0/5 14 * * MON-FRI", zone = "Asia/Seoul")
    fun updateCurrency() {
        try {
            val todayString = DateUtils.getTodayForApi()
            val apiDate = DateUtils.parseApiDate(todayString)
            exchangeRateService.syncExchangeRates(todayString, apiDate)
        } catch (e: Exception) {
            log.error("Failed to update exchange rate: {}", e.message, e)
        }
    }
}
