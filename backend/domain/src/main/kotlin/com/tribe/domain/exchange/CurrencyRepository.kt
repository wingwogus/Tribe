package com.tribe.domain.exchange

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/**
 * 환율 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface CurrencyRepository : JpaRepository<Currency, CurrencyId> {
    fun findByCurUnitAndDate(curUnit: String, date: LocalDate): Currency?
    fun findTopByCurUnitOrderByDateDesc(curUnit: String): Currency?
    fun findTopByCurUnitAndDateLessThanEqualOrderByDateDesc(curUnit: String, date: LocalDate): Currency?
    fun findTopByCurUnitAndDateGreaterThanEqualOrderByDateAsc(curUnit: String, date: LocalDate): Currency?
}
