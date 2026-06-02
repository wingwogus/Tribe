package com.tribe.application.exchange

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 환율 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
data class ExchangeRatePayload(
    val result: Int,
    @JsonProperty("cur_unit")
    val curUnit: String,
    @JsonProperty("cur_nm")
    val curName: String,
    @JsonProperty("deal_bas_r")
    val dealBasR: String,
)
