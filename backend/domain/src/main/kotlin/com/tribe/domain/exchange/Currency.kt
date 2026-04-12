package com.tribe.domain.exchange

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "currency")
@IdClass(CurrencyId::class)
class Currency(
    @Id
    @Column(name = "cur_unit", length = 10)
    var curUnit: String,

    @Id
    @Column(nullable = false)
    var date: LocalDate,

    @Column(nullable = false)
    var curName: String,

    @Column(nullable = false, precision = 10, scale = 4)
    var exchangeRate: BigDecimal,
) {
    constructor() : this("", LocalDate.MIN, "", BigDecimal.ZERO)
}
