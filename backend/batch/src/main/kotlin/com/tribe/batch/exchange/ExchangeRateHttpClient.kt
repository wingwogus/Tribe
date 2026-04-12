package com.tribe.batch.exchange

import com.tribe.application.exchange.ExchangeRateGateway
import com.tribe.application.exchange.ExchangeRatePayload
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class ExchangeRateHttpClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${exchange.rate.api-url}") baseUrl: String,
) : ExchangeRateGateway {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = restClientBuilder.baseUrl(baseUrl).build()
    private val payloadType = object : ParameterizedTypeReference<List<ExchangeRatePayload>>() {}

    override fun findExchange(
        authKey: String,
        searchDate: String,
        data: String,
    ): List<ExchangeRatePayload> {
        return restClient.get()
            .uri { builder ->
                builder
                    .path("/site/program/financial/exchangeJSON")
                    .queryParam("authkey", authKey)
                    .queryParam("searchdate", searchDate)
                    .queryParam("data", data)
                    .build()
            }
            .retrieve()
            .body(payloadType)
            ?.also { log.info("Exchange API returned {} rows for {}", it.size, searchDate) }
            ?: emptyList()
    }
}
