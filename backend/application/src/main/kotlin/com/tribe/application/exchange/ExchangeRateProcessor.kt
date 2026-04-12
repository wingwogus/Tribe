package com.tribe.application.exchange

import com.tribe.domain.exchange.Currency
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object ExchangeRateProcessor {
    private val log = LoggerFactory.getLogger(ExchangeRateProcessor::class.java)

    fun process(dto: ExchangeRatePayload, date: LocalDate): Currency? {
        val rawCurrencyUnit = dto.curUnit
        val is100Unit = rawCurrencyUnit.endsWith("(100)")
        val targetCurrency = when {
            is100Unit -> rawCurrencyUnit.substringBefore('(')
            rawCurrencyUnit.contains('(') || rawCurrencyUnit.contains(')') -> return null
            else -> rawCurrencyUnit
        }

        if (targetCurrency == "KRW") {
            return null
        }

        var exchangeRate = try {
            BigDecimal(dto.dealBasR.replace(",", ""))
        } catch (e: NumberFormatException) {
            log.warn("Failed to parse exchange rate for {}: {}", dto.curUnit, dto.dealBasR)
            return null
        }

        if (is100Unit) {
            exchangeRate = exchangeRate.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        }

        return Currency(
            curUnit = targetCurrency,
            curName = dto.curName,
            exchangeRate = exchangeRate.setScale(4, RoundingMode.HALF_UP),
            date = date,
        )
    }
}
