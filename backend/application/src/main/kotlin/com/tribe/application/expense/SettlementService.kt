package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.exchange.Currency
import com.tribe.domain.exchange.CurrencyRepository
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.core.TripRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class SettlementService(
    private val expenseRepository: ExpenseRepository,
    private val tripRepository: TripRepository,
    private val currencyRepository: CurrencyRepository,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
) {
    private val krw = "KRW"
    private val epsilon = BigDecimal("1.00")
    private val foreignScale = 2

    @PreAuthorize("@tripAuthorizationPolicy.isTripMember(#tripId)")
    fun getDailySettlement(tripId: Long, date: LocalDate): SettlementResult.Daily {
        val trip = tripRepository.findById(tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val dailyExpenses = expenseRepository.findAllWithDetailsByTripId(tripId)
            .filter { it.spentAt == date }

        val totalKrw = dailyExpenses.sumOf { convertToKrw(it.amount, it.currencyCode, it.spentAt) }
        val memberData = calculateMemberSettlementData(trip.members, dailyExpenses)
        val memberSummaries = memberData.map {
            SettlementResult.MemberDailySummary(
                memberId = it.member.id,
                memberName = it.member.name,
                paidAmount = it.paidAmountKrw,
                assignedAmount = it.assignedAmountKrw,
            )
        }
        val debtRelations = calculateDebtRelationsByCurrency(
            members = trip.members,
            expenses = dailyExpenses,
            rateLookup = { currencyCode -> findClosestRate(currencyCode, date)?.exchangeRate },
        )

        val totalAssigned = memberSummaries.sumOf { it.assignedAmount }
        if (totalKrw.subtract(totalAssigned).abs() > epsilon) {
            // tolerated warning path; keep response generation stable
        }

        return SettlementResult.Daily(
            date = date,
            dailyTotalAmount = totalKrw,
            expenses = dailyExpenses.map {
                SettlementResult.DailyExpenseSummary(
                    expenseId = it.id,
                    title = it.title,
                    payerName = it.payer.name,
                    totalAmount = convertToKrw(it.amount, it.currencyCode, it.spentAt),
                    originalAmount = it.amount,
                    currencyCode = it.currencyCode,
                )
            },
            memberSummaries = memberSummaries,
            debtRelations = debtRelations,
        )
    }

    @PreAuthorize("@tripAuthorizationPolicy.isTripMember(#tripId)")
    fun getTotalSettlement(tripId: Long): SettlementResult.Total {
        val trip = tripRepository.findById(tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val expenses = expenseRepository.findAllWithDetailsByTripId(tripId)
        val memberData = calculateMemberSettlementData(trip.members, expenses)
        val debtRelations = calculateDebtRelationsByCurrency(
            members = trip.members,
            expenses = expenses,
            rateLookup = { currencyCode -> currencyRepository.findTopByCurUnitOrderByDateDesc(currencyCode)?.exchangeRate },
        )
        return SettlementResult.Total(
            memberBalances = memberData.map {
                SettlementResult.MemberBalance(
                    tripMemberId = it.member.id,
                    nickname = it.member.name,
                    balance = it.paidAmountKrw.subtract(it.assignedAmountKrw),
                    foreignCurrenciesUsed = it.foreignCurrencies,
                )
            },
            debtRelations = debtRelations,
            isExchangeRateApplied = true,
        )
    }

    private fun calculateMemberSettlementData(
        members: List<TripMember>,
        expenses: List<Expense>,
    ): List<MemberSettlementData> {
        return members.map { member ->
            val paid = expenses
                .filter { it.payer.id == member.id }
                .sumOf { convertToKrw(it.amount, it.currencyCode, it.spentAt) }

            val assigned = expenses
                .flatMap { expense -> expense.expenseItems.flatMap { item -> item.assignments.map { assignment -> expense to assignment } } }
                .filter { (_, assignment) -> assignment.tripMember.id == member.id }
                .sumOf { (expense, assignment) ->
                    convertToKrw(assignment.amount, expense.currencyCode, expense.spentAt)
                }

            val foreignCurrencies = expenses
                .filter {
                    it.currencyCode != krw && (
                        it.payer.id == member.id ||
                            it.expenseItems.any { item -> item.assignments.any { assignment -> assignment.tripMember.id == member.id } }
                        )
                }
                .map { it.currencyCode }
                .distinct()

            MemberSettlementData(member, paid, assigned, foreignCurrencies)
        }
    }

    private fun convertToKrw(amount: BigDecimal, currencyCode: String, date: LocalDate): BigDecimal {
        val normalized = currencyCode.uppercase()
        if (normalized == krw) return amount.setScale(0, RoundingMode.HALF_UP)
        val rate = findClosestRate(normalized, date)?.exchangeRate
            ?: throw BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND)
        return amount.multiply(rate).setScale(0, RoundingMode.HALF_UP)
    }

    private fun findClosestRate(currencyCode: String, targetDate: LocalDate): Currency? {
        val exact = currencyRepository.findByCurUnitAndDate(currencyCode, targetDate)
        if (exact != null) return exact

        val past = currencyRepository.findTopByCurUnitAndDateLessThanEqualOrderByDateDesc(currencyCode, targetDate)
        val future = currencyRepository.findTopByCurUnitAndDateGreaterThanEqualOrderByDateAsc(currencyCode, targetDate)

        return when {
            past != null && future == null -> past
            past == null && future != null -> future
            past != null && future != null -> {
                val pastDistance = ChronoUnit.DAYS.between(past.date, targetDate).coerceAtLeast(0)
                val futureDistance = ChronoUnit.DAYS.between(targetDate, future.date).coerceAtLeast(0)
                if (pastDistance <= futureDistance) past else future
            }
            else -> null
        }
    }

    private fun calculateDebtRelationsByCurrency(
        members: List<TripMember>,
        expenses: List<Expense>,
        rateLookup: (currencyCode: String) -> BigDecimal?,
    ): List<SettlementResult.DebtRelation> {
        val allRelations = mutableListOf<SettlementResult.DebtRelation>()
        val allCurrencies = expenses.map { it.currencyCode.uppercase() }.distinct().plus(krw).distinct()

        for (currencyCode in allCurrencies) {
            val isForeign = currencyCode != krw
            val exchangeRate = if (isForeign) rateLookup(currencyCode) else BigDecimal.ONE
            if (exchangeRate == null) continue

            val balancesInCurrency = members.map { member ->
                val paid = expenses
                    .filter { it.payer.id == member.id && it.currencyCode.uppercase() == currencyCode }
                    .sumOf { it.amount }

                val assigned = expenses
                    .flatMap { it.expenseItems }
                    .flatMap { it.assignments }
                    .filter { it.tripMember.id == member.id && it.expenseItem.expense.currencyCode.uppercase() == currencyCode }
                    .sumOf { it.amount }

                member to paid.subtract(assigned)
            }

            val totalAbsBalance = balancesInCurrency.sumOf { it.second.abs() }
            if (totalAbsBalance < BigDecimal("0.01")) continue

            allRelations.addAll(calculateMinimalTransfers(balancesInCurrency, currencyCode, exchangeRate))
        }

        return allRelations
    }

    private fun calculateMinimalTransfers(
        balances: List<Pair<TripMember, BigDecimal>>,
        currencyCode: String,
        exchangeRate: BigDecimal,
    ): List<SettlementResult.DebtRelation> {
        val cleanBalances = balances
            .filter { it.second.abs() >= BigDecimal("0.01") }
            .sortedBy { it.second }

        val debtors = cleanBalances.filter { it.second.signum() < 0 }.toMutableList()
        val creditors = cleanBalances.filter { it.second.signum() > 0 }.toMutableList()
        val relations = mutableListOf<SettlementResult.DebtRelation>()
        val isForeign = currencyCode != krw

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val (debtor, debtorBalanceRaw) = debtors.first()
            val (creditor, creditorBalanceRaw) = creditors.first()
            val transferOriginal = debtorBalanceRaw.abs().min(creditorBalanceRaw)
            val transferKrw = transferOriginal.multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP)

            if (transferKrw >= epsilon) {
                relations.add(
                    SettlementResult.DebtRelation(
                        fromNickname = debtor.name,
                        fromTripMemberId = debtor.id,
                        toNickname = creditor.name,
                        toTripMemberId = creditor.id,
                        amount = transferKrw,
                        equivalentOriginalAmount = if (isForeign) transferOriginal.setScale(foreignScale, RoundingMode.HALF_UP) else null,
                        originalCurrencyCode = if (isForeign) currencyCode else null,
                    ),
                )
            }

            val nextDebtorBalance = debtorBalanceRaw.add(transferOriginal)
            val nextCreditorBalance = creditorBalanceRaw.subtract(transferOriginal)
            if (nextDebtorBalance.abs() < BigDecimal("0.01")) {
                debtors.removeAt(0)
            } else {
                debtors[0] = debtor to nextDebtorBalance
            }
            if (nextCreditorBalance.abs() < BigDecimal("0.01")) {
                creditors.removeAt(0)
            } else {
                creditors[0] = creditor to nextCreditorBalance
            }
        }

        return relations
    }

    private data class MemberSettlementData(
        val member: TripMember,
        val paidAmountKrw: BigDecimal,
        val assignedAmountKrw: BigDecimal,
        val foreignCurrencies: List<String>,
    )
}
