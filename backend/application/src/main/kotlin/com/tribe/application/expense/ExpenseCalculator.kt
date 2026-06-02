package com.tribe.application.expense

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 지출 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
object ExpenseCalculator {
    fun calculateFairShare(total: BigDecimal, participantCount: Int): List<BigDecimal> {
        if (participantCount <= 0) return emptyList()

        val baseShare = total
            .divide(BigDecimal(participantCount), 0, RoundingMode.DOWN)
        val remainder = total.subtract(baseShare.multiply(BigDecimal(participantCount)))
        val result = MutableList(participantCount) { baseShare }

        var index = 0
        var remaining = remainder
        while (remaining > BigDecimal.ZERO) {
            result[index] = result[index].add(BigDecimal.ONE)
            remaining = remaining.subtract(BigDecimal.ONE)
            index++
        }
        while (remaining < BigDecimal.ZERO) {
            result[index] = result[index].subtract(BigDecimal.ONE)
            remaining = remaining.add(BigDecimal.ONE)
            index++
        }

        return result
    }
}
