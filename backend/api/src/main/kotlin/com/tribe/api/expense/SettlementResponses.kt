package com.tribe.api.expense

import com.tribe.application.expense.SettlementResult
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지출 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object SettlementResponses {
    data class DailyExpenseSummaryResponse(
        val expenseId: Long,
        val title: String,
        val payerName: String,
        val totalAmount: BigDecimal,
        val originalAmount: BigDecimal,
        val currencyCode: String,
    )

    data class MemberDailySummaryResponse(
        val memberId: Long,
        val memberName: String,
        val paidAmount: BigDecimal,
        val assignedAmount: BigDecimal,
    )

    data class DebtRelationResponse(
        val fromNickname: String,
        val fromTripMemberId: Long,
        val toNickname: String,
        val toTripMemberId: Long,
        val amount: BigDecimal,
        val equivalentOriginalAmount: BigDecimal? = null,
        val originalCurrencyCode: String? = null,
    )

    data class DailyResponse(
        val date: LocalDate,
        val dailyTotalAmount: BigDecimal,
        val expenses: List<DailyExpenseSummaryResponse>,
        val memberSummaries: List<MemberDailySummaryResponse>,
        val debtRelations: List<DebtRelationResponse>,
    )

    data class MemberBalanceResponse(
        val tripMemberId: Long,
        val nickname: String,
        val balance: BigDecimal,
        val foreignCurrenciesUsed: List<String>,
    )

    data class TotalResponse(
        val memberBalances: List<MemberBalanceResponse>,
        val debtRelations: List<DebtRelationResponse>,
        val isExchangeRateApplied: Boolean,
    )

    fun from(result: SettlementResult.Daily): DailyResponse = DailyResponse(
        date = result.date,
        dailyTotalAmount = result.dailyTotalAmount,
        expenses = result.expenses.map {
            DailyExpenseSummaryResponse(it.expenseId, it.title, it.payerName, it.totalAmount, it.originalAmount, it.currencyCode)
        },
        memberSummaries = result.memberSummaries.map {
            MemberDailySummaryResponse(it.memberId, it.memberName, it.paidAmount, it.assignedAmount)
        },
        debtRelations = result.debtRelations.map {
            DebtRelationResponse(it.fromNickname, it.fromTripMemberId, it.toNickname, it.toTripMemberId, it.amount, it.equivalentOriginalAmount, it.originalCurrencyCode)
        },
    )

    fun from(result: SettlementResult.Total): TotalResponse = TotalResponse(
        memberBalances = result.memberBalances.map {
            MemberBalanceResponse(it.tripMemberId, it.nickname, it.balance, it.foreignCurrenciesUsed)
        },
        debtRelations = result.debtRelations.map {
            DebtRelationResponse(it.fromNickname, it.fromTripMemberId, it.toNickname, it.toTripMemberId, it.amount, it.equivalentOriginalAmount, it.originalCurrencyCode)
        },
        isExchangeRateApplied = result.isExchangeRateApplied,
    )
}
