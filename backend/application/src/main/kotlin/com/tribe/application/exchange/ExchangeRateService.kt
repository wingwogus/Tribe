package com.tribe.application.exchange

import com.tribe.domain.exchange.Currency
import com.tribe.domain.exchange.CurrencyRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnBean(ExchangeRateGateway::class)
class ExchangeRateService(
    private val exchangeRateGateway: ExchangeRateGateway,
    private val currencyRepository: CurrencyRepository,
    @Value("\${exchange.rate.key}") private val authKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun fetchAndSaveExchangeRate(date: LocalDate): Currency? {
        val currencies = fetchAndConvert(date.format(formatter), date)
        if (currencies.isEmpty()) {
            return null
        }

        currencyRepository.saveAll(currencies)
        log.info("On-demand saved/updated {} exchange rates for {}", currencies.size, date)
        return currencies.firstOrNull()
    }

    @Transactional
    fun syncExchangeRates(searchDate: String, apiDate: LocalDate): Int {
        val currencies = fetchAndConvert(searchDate, apiDate)
        if (currencies.isEmpty()) {
            return 0
        }

        currencyRepository.saveAll(currencies)
        log.info("Saved/updated {} exchange rates for {}", currencies.size, apiDate)
        return currencies.size
    }

    private fun fetchAndConvert(searchDate: String, apiDate: LocalDate): List<Currency> {
        val payloads = try {
            exchangeRateGateway.findExchange(authKey, searchDate)
        } catch (e: Exception) {
            log.error("Exchange API call failed for {}: {}", searchDate, e.message, e)
            return emptyList()
        }

        if (payloads.isEmpty()) {
            log.warn("Exchange API returned empty response for {}", searchDate)
            return emptyList()
        }

        return payloads.mapNotNull { ExchangeRateProcessor.process(it, apiDate) }
    }
}
