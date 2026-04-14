package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseAssignment
import com.tribe.domain.expense.ExpenseCategory
import com.tribe.domain.expense.ExpenseItem
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.expense.ExpenseSplitType
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import com.tribe.domain.member.Member
import com.tribe.domain.expense.ExpenseItemRepository
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ExpenseServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var expenseRepository: ExpenseRepository
    @Mock private lateinit var expenseItemRepository: ExpenseItemRepository
    @Mock private lateinit var itineraryItemRepository: ItineraryItemRepository
    @Mock private lateinit var expenseReceiptAnalyzer: ExpenseReceiptAnalyzer
    @Mock private lateinit var expenseReceiptStorage: ExpenseReceiptStorage

    private lateinit var expenseAuthorizationPolicy: ExpenseAuthorizationPolicy
    private lateinit var expenseService: ExpenseService

    @BeforeEach
    fun setUp() {
        expenseAuthorizationPolicy = ExpenseAuthorizationPolicy(currentActor, tripRepository)
        expenseService = ExpenseService(
            expenseAuthorizationPolicy = expenseAuthorizationPolicy,
            expenseRepository = expenseRepository,
            expenseItemRepository = expenseItemRepository,
            itineraryItemRepository = itineraryItemRepository,
            expenseReceiptAnalyzer = expenseReceiptAnalyzer,
            expenseReceiptStorage = expenseReceiptStorage,
        )
    }

    @Test
    fun `createExpense creates handwrite expense with line items`() {
        val fixture = expenseFixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.actor.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(itineraryItemRepository.findById(fixture.itineraryItem.id)).thenReturn(java.util.Optional.of(fixture.itineraryItem))
        doAnswer { invocation ->
            val saved = invocation.arguments[0] as Expense
            ReflectionTestUtils.setField(saved, "id", 99L)
            saved.expenseItems.forEachIndexed { index, item ->
                ReflectionTestUtils.setField(item, "id", (index + 1).toLong())
            }
            saved
        }.`when`(expenseRepository).save(org.mockito.ArgumentMatchers.any(Expense::class.java))

        val result = expenseService.createExpense(
            ExpenseCommand.Create(
                tripId = fixture.trip.id,
                title = "Dinner",
                amount = BigDecimal("50000"),
                currencyCode = "krw",
                spentAt = LocalDate.of(2026, 4, 12),
                category = "food",
                splitType = "equal",
                payerTripMemberId = fixture.payerMembership.id,
                itineraryItemId = fixture.itineraryItem.id,
                inputMethod = "handwrite",
                items = listOf(
                    ExpenseCommand.Item(itemName = "Ramen", price = BigDecimal("20000")),
                    ExpenseCommand.Item(itemName = "Beer", price = BigDecimal("30000")),
                ),
            ),
        )

        assertEquals(99L, result.expenseId)
        assertEquals("HANDWRITE", result.inputMethod)
        assertEquals(2, result.items.size)
        assertEquals("Dinner", result.title)
        assertEquals(1L, result.itineraryItemId)
    }

    @Test
    fun `createExpense scan adds remainder item and uploads receipt`() {
        val fixture = expenseFixture()
        val receiptBytes = "image".toByteArray()
        `when`(currentActor.requireUserId()).thenReturn(fixture.actor.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(itineraryItemRepository.findById(fixture.itineraryItem.id)).thenReturn(java.util.Optional.of(fixture.itineraryItem))
        `when`(expenseReceiptStorage.upload(receiptBytes, "receipts", "image/jpeg"))
            .thenReturn("https://cdn/receipt.jpg")
        `when`(expenseReceiptAnalyzer.analyze(receiptBytes, "image/jpeg"))
            .thenReturn(
                ReceiptAnalysis(
                    totalAmount = BigDecimal("1200"),
                    items = listOf(
                        ReceiptItem("파스타", BigDecimal("700")),
                        ReceiptItem("와인", BigDecimal("300")),
                    ),
                ),
            )
        doAnswer { invocation ->
            val saved = invocation.arguments[0] as Expense
            ReflectionTestUtils.setField(saved, "id", 77L)
            saved.expenseItems.forEachIndexed { index, item ->
                ReflectionTestUtils.setField(item, "id", (index + 1).toLong())
            }
            saved
        }.`when`(expenseRepository).save(org.mockito.ArgumentMatchers.any(Expense::class.java))

        val result = expenseService.createExpense(
            ExpenseCommand.Create(
                tripId = fixture.trip.id,
                title = "Receipt",
                amount = null,
                currencyCode = "usd",
                spentAt = LocalDate.of(2026, 4, 12),
                category = "food",
                splitType = "equal",
                payerTripMemberId = fixture.payerMembership.id,
                itineraryItemId = fixture.itineraryItem.id,
                inputMethod = "scan",
                receiptImageBytes = receiptBytes,
                receiptImageContentType = "image/jpeg",
            ),
        )

        assertEquals(BigDecimal("1200"), result.amount)
        assertEquals(3, result.items.size)
        assertEquals("https://cdn/receipt.jpg", result.receiptImageUrl)
        assertEquals("세금 / 팁 / 기타", result.items.last().itemName)
    }

    @Test
    fun `assignParticipants distributes item amount equally`() {
        val fixture = expenseFixture()
        val expense = expenseWithItems(fixture)
        `when`(currentActor.requireUserId()).thenReturn(fixture.actor.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.findWithDetailsById(expense.id)).thenReturn(expense)

        val result = expenseService.assignParticipants(
            ExpenseCommand.AssignParticipants(
                tripId = fixture.trip.id,
                expenseId = expense.id,
                items = listOf(
                    ExpenseCommand.ItemAssignment(
                        itemId = expense.expenseItems.first().id,
                        participantIds = listOf(fixture.payerMembership.id, fixture.memberMembership.id),
                    ),
                ),
            ),
        )

        assertEquals(1, result.items.size)
        assertEquals(2, result.items.first().participants.size)
        assertEquals(BigDecimal("10000"), result.items.first().participants.first().amount)
    }

    @Test
    fun `clearAssignments removes item allocations`() {
        val fixture = expenseFixture()
        val expense = expenseWithItems(fixture, withAssignments = true)
        `when`(currentActor.requireUserId()).thenReturn(fixture.actor.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.findWithDetailsById(expense.id)).thenReturn(expense)

        val result = expenseService.clearAssignments(
            ExpenseCommand.ClearAssignments(
                tripId = fixture.trip.id,
                expenseId = expense.id,
                itemIds = listOf(expense.expenseItems.first().id),
            ),
        )

        assertEquals(0, result.items.first().participants.size)
    }

    @Test
    fun `updateExpense rejects mismatched total and item sum`() {
        val fixture = expenseFixture()
        val expense = expenseWithItems(fixture)
        `when`(currentActor.requireUserId()).thenReturn(fixture.actor.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.findWithDetailsById(expense.id)).thenReturn(expense)

        val ex = assertThrows(BusinessException::class.java) {
            expenseService.updateExpense(
                ExpenseCommand.Update(
                    tripId = fixture.trip.id,
                    expenseId = expense.id,
                    title = "Updated",
                    amount = BigDecimal("100"),
                    currencyCode = "KRW",
                    spentAt = LocalDate.of(2026, 4, 12),
                    category = "FOOD",
                    splitType = "EQUAL",
                    payerTripMemberId = fixture.payerMembership.id,
                    itineraryItemId = fixture.itineraryItem.id,
                    inputMethod = "HANDWRITE",
                    items = listOf(
                        ExpenseCommand.Item(
                            itemId = expense.expenseItems.first().id,
                            itemName = "Only",
                            price = BigDecimal("90"),
                        ),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
    }

    private fun expenseFixture(): ExpenseFixture {
        val trip = Trip(
            title = "Tokyo",
            startDate = LocalDate.of(2026, 4, 10),
            endDate = LocalDate.of(2026, 4, 14),
            country = com.tribe.domain.trip.core.Country.JAPAN,
        )
        ReflectionTestUtils.setField(trip, "id", 10L)

        val actor = Member(id = 1L, email = "actor@example.com", passwordHash = "hashed", nickname = "payer")
        val member = Member(id = 2L, email = "member@example.com", passwordHash = "hashed", nickname = "member")

        val payerMembership = TripMember(member = actor, trip = trip, role = TripRole.MEMBER)
        val memberMembership = TripMember(member = member, trip = trip, role = TripRole.MEMBER)
        ReflectionTestUtils.setField(payerMembership, "id", 11L)
        ReflectionTestUtils.setField(memberMembership, "id", 12L)

        trip.members.add(payerMembership)
        trip.members.add(memberMembership)

        val itineraryItem = ItineraryItem(trip, 1, null, "Dinner", null, 1, null)
        ReflectionTestUtils.setField(itineraryItem, "id", 1L)

        return ExpenseFixture(trip, actor, payerMembership, memberMembership, itineraryItem)
    }

    private fun expenseWithItems(fixture: ExpenseFixture, withAssignments: Boolean = false): Expense {
        val expense = Expense(
            trip = fixture.trip,
            itineraryItem = fixture.itineraryItem,
            createdBy = fixture.actor,
            payer = fixture.payerMembership,
            title = "Dinner",
            amount = BigDecimal("20000"),
            currencyCode = "KRW",
            spentAt = LocalDate.of(2026, 4, 12),
            category = ExpenseCategory.FOOD,
            splitType = ExpenseSplitType.EQUAL,
        )
        ReflectionTestUtils.setField(expense, "id", 99L)

        val item = ExpenseItem(
            expense = expense,
            name = "Item",
            price = BigDecimal("20000"),
        )
        ReflectionTestUtils.setField(item, "id", 201L)
        if (withAssignments) {
            item.assignments.add(
                ExpenseAssignment(item, fixture.payerMembership, BigDecimal("10000")),
            )
            item.assignments.add(
                ExpenseAssignment(item, fixture.memberMembership, BigDecimal("10000")),
            )
        }
        expense.expenseItems.add(item)
        return expense
    }

    private data class ExpenseFixture(
        val trip: Trip,
        val actor: Member,
        val payerMembership: TripMember,
        val memberMembership: TripMember,
        val itineraryItem: ItineraryItem,
    )
}
