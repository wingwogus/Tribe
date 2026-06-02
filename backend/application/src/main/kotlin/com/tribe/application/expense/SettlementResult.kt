package com.tribe.application.expense

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지출 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object SettlementResult {
    data class DailyExpenseSummary(
        val expenseId: Long,
        val title: String,
        val payerName: String,
        val totalAmount: BigDecimal,
        val originalAmount: BigDecimal,
        val currencyCode: String,
    )

    data class MemberDailySummary(
        val memberId: Long,
        val memberName: String,
        val paidAmount: BigDecimal,
        val assignedAmount: BigDecimal,
    )

    data class MemberBalance(
        val tripMemberId: Long,
        val nickname: String,
        val balance: BigDecimal,
        val foreignCurrenciesUsed: List<String> = emptyList(),
    )

    data class DebtRelation(
        val fromNickname: String,
        val fromTripMemberId: Long,
        val toNickname: String,
        val toTripMemberId: Long,
        val amount: BigDecimal,
        val equivalentOriginalAmount: BigDecimal? = null,
        val originalCurrencyCode: String? = null,
    )

    data class Daily(
        val date: LocalDate,
        val dailyTotalAmount: BigDecimal,
        val expenses: List<DailyExpenseSummary>,
        val memberSummaries: List<MemberDailySummary>,
        val debtRelations: List<DebtRelation>,
    )

    data class Total(
        val memberBalances: List<MemberBalance>,
        val debtRelations: List<DebtRelation>,
        val isExchangeRateApplied: Boolean = true,
    )
}
