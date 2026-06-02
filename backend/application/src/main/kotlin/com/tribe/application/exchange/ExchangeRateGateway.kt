package com.tribe.application.exchange

/**
 * 환율 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
interface ExchangeRateGateway {
    fun findExchange(
        authKey: String,
        searchDate: String,
        data: String = "AP01",
    ): List<ExchangeRatePayload>
}
