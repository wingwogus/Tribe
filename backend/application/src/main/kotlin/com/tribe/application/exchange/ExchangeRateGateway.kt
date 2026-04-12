package com.tribe.application.exchange

interface ExchangeRateGateway {
    fun findExchange(
        authKey: String,
        searchDate: String,
        data: String = "AP01",
    ): List<ExchangeRatePayload>
}
