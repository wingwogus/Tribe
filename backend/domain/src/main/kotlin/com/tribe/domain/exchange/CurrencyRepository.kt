package com.tribe.domain.exchange

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface CurrencyRepository : JpaRepository<Currency, CurrencyId> {
    fun findByCurUnitAndDate(curUnit: String, date: LocalDate): Currency?
    fun findTopByCurUnitOrderByDateDesc(curUnit: String): Currency?
    fun findTopByCurUnitAndDateLessThanEqualOrderByDateDesc(curUnit: String, date: LocalDate): Currency?
    fun findTopByCurUnitAndDateGreaterThanEqualOrderByDateAsc(curUnit: String, date: LocalDate): Currency?
}
