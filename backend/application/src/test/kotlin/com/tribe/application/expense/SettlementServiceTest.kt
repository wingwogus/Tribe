package com.tribe.application.expense

import com.tribe.domain.exchange.Currency
import com.tribe.domain.exchange.CurrencyRepository
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseAssignment
import com.tribe.domain.expense.ExpenseCategory
import com.tribe.domain.expense.ExpenseItem
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.expense.ExpenseSplitType
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import com.tribe.domain.member.Member
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SettlementServiceTest {
    @Mock private lateinit var expenseRepository: ExpenseRepository
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var currencyRepository: CurrencyRepository
    @Mock private lateinit var tripAuthorizationPolicy: com.tribe.application.trip.core.TripAuthorizationPolicy

    private lateinit var settlementService: SettlementService

    @BeforeEach
    fun setUp() {
        settlementService = SettlementService(
            expenseRepository = expenseRepository,
            tripRepository = tripRepository,
            currencyRepository = currencyRepository,
            tripAuthorizationPolicy = tripAuthorizationPolicy,
        )
    }

    @Test
    fun `total settlement keeps original currency metadata in debt relations`() {
        val fixture = settlementFixture()
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(expenseRepository.findAllWithDetailsByTripId(fixture.trip.id)).thenReturn(fixture.expenses)
        `when`(currencyRepository.findByCurUnitAndDate("JPY", fixture.date)).thenReturn(
            Currency("JPY", fixture.date, "일본 엔", BigDecimal("9.31")),
        )
        `when`(currencyRepository.findTopByCurUnitOrderByDateDesc("JPY")).thenReturn(
            Currency("JPY", fixture.date, "일본 엔", BigDecimal("9.31")),
        )

        val result = settlementService.getTotalSettlement(fixture.trip.id)

        assertEquals(2, result.debtRelations.size)
        assertEquals("JPY", result.debtRelations.first().originalCurrencyCode)
        assertEquals(BigDecimal("700.00"), result.debtRelations.first().equivalentOriginalAmount)
    }

    private fun settlementFixture(): SettlementFixture {
        val date = LocalDate.of(2025, 10, 27)
        val trip = Trip("정산 테스트", date, date.plusDays(1), Country.JAPAN)
        ReflectionTestUtils.setField(trip, "id", 10L)

        val userA = Member(id = 1L, email = "a@test.com", passwordHash = "hashed", nickname = "A")
        val userB = Member(id = 2L, email = "b@test.com", passwordHash = "hashed", nickname = "B")

        val memberA = TripMember(userA, trip, role = TripRole.OWNER)
        val memberB = TripMember(userB, trip, role = TripRole.MEMBER)
        val guestC = TripMember(null, trip, guestNickname = "게스트C", role = TripRole.GUEST)
        ReflectionTestUtils.setField(memberA, "id", 11L)
        ReflectionTestUtils.setField(memberB, "id", 12L)
        ReflectionTestUtils.setField(guestC, "id", 13L)
        trip.members.add(memberA)
        trip.members.add(memberB)
        trip.members.add(guestC)

        val dinnerExpense = Expense(
            trip = trip,
            createdBy = userA,
            payer = memberA,
            title = "저녁",
            amount = BigDecimal("3000"),
            currencyCode = "JPY",
            spentAt = date,
            category = ExpenseCategory.FOOD,
            splitType = ExpenseSplitType.EQUAL,
        )
        ReflectionTestUtils.setField(dinnerExpense, "id", 21L)
        val dinnerItem = ExpenseItem(dinnerExpense, "저녁메뉴", BigDecimal("3000"))
        ReflectionTestUtils.setField(dinnerItem, "id", 31L)
        dinnerItem.assignments.add(ExpenseAssignment(dinnerItem, memberA, BigDecimal("1500")))
        dinnerItem.assignments.add(ExpenseAssignment(dinnerItem, memberB, BigDecimal("1500")))
        dinnerExpense.expenseItems.add(dinnerItem)

        val snackExpense = Expense(
            trip = trip,
            createdBy = userB,
            payer = memberB,
            title = "간식",
            amount = BigDecimal("1200"),
            currencyCode = "JPY",
            spentAt = date,
            category = ExpenseCategory.FOOD,
            splitType = ExpenseSplitType.EQUAL,
        )
        ReflectionTestUtils.setField(snackExpense, "id", 22L)
        val snackItem = ExpenseItem(snackExpense, "간식메뉴", BigDecimal("1200"))
        ReflectionTestUtils.setField(snackItem, "id", 32L)
        snackItem.assignments.add(ExpenseAssignment(snackItem, memberA, BigDecimal("400")))
        snackItem.assignments.add(ExpenseAssignment(snackItem, memberB, BigDecimal("400")))
        snackItem.assignments.add(ExpenseAssignment(snackItem, guestC, BigDecimal("400")))
        snackExpense.expenseItems.add(snackItem)

        return SettlementFixture(trip, date, listOf(dinnerExpense, snackExpense))
    }

    private data class SettlementFixture(
        val trip: Trip,
        val date: LocalDate,
        val expenses: List<Expense>,
    )
}
