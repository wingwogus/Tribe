package com.tribe.application.exchange

import com.fasterxml.jackson.annotation.JsonProperty

data class ExchangeRatePayload(
    val result: Int,
    @JsonProperty("cur_unit")
    val curUnit: String,
    @JsonProperty("cur_nm")
    val curName: String,
    @JsonProperty("deal_bas_r")
    val dealBasR: String,
)
