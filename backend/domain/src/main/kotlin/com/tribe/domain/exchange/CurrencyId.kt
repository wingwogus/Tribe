package com.tribe.domain.exchange

import java.io.Serializable
import java.time.LocalDate

/**
 * 환율 도메인 상태 모델.
 *
 * 영속성 identity와 업무 규칙의 기준점.
 */
class CurrencyId(
    var curUnit: String = "",
    var date: LocalDate = LocalDate.MIN,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurrencyId) return false
        return curUnit == other.curUnit && date == other.date
    }

    override fun hashCode(): Int = 31 * curUnit.hashCode() + date.hashCode()
}
