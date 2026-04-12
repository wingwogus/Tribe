package com.tribe.application.trip.member

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.application.trip.core.TripCommand
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseAssignment
import com.tribe.domain.expense.ExpenseCategory
import com.tribe.domain.expense.ExpenseItem
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.expense.ExpenseSplitType
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class TripMemberIntegrityServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripAuthorizationPolicy: TripAuthorizationPolicy
    @Mock private lateinit var tripRealtimeEventPublisher: TripRealtimeEventPublisher
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var expenseRepository: ExpenseRepository
    @Mock private lateinit var wishlistItemRepository: WishlistItemRepository

    private lateinit var service: TripMemberIntegrityService

    @BeforeEach
    fun setUp() {
        service = TripMemberIntegrityService(
            currentActor = currentActor,
            tripAuthorizationPolicy = tripAuthorizationPolicy,
            tripRealtimeEventPublisher = tripRealtimeEventPublisher,
            tripRepository = tripRepository,
            expenseRepository = expenseRepository,
            wishlistItemRepository = wishlistItemRepository,
        )
    }

    @Test
    fun `deleteGuest reassigns payer and redistributes assignments`() {
        val fixture = fixture()
        val expense = guestExpense(fixture, withRemainingMembers = true)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.findAllWithDetailsByTripId(fixture.trip.id)).thenReturn(listOf(expense))
        `when`(tripAuthorizationPolicy.isTripAdmin(fixture.trip.id)).thenReturn(true)

        val result = service.deleteGuest(TripCommand.DeleteGuest(fixture.trip.id, fixture.guest.id))

        assertEquals(fixture.owner.id, expense.payer.id)
        assertEquals(2, expense.expenseItems.first().assignments.size)
        assertEquals(BigDecimal("5000"), expense.expenseItems.first().assignments.first().amount)
        assertEquals(2, result.members.size)
        verify(wishlistItemRepository).deleteByAdderId(fixture.guest.id)
    }

    @Test
    fun `deleteGuest assigns full item to payer when guest was sole assignee`() {
        val fixture = fixture()
        val expense = guestExpense(fixture, withRemainingMembers = false)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.findAllWithDetailsByTripId(fixture.trip.id)).thenReturn(listOf(expense))
        `when`(tripAuthorizationPolicy.isTripAdmin(fixture.trip.id)).thenReturn(true)

        service.deleteGuest(TripCommand.DeleteGuest(fixture.trip.id, fixture.guest.id))

        assertEquals(fixture.owner.id, expense.payer.id)
        assertEquals(1, expense.expenseItems.first().assignments.size)
        assertEquals(fixture.owner.id, expense.expenseItems.first().assignments.first().tripMember.id)
        assertEquals(BigDecimal("10000"), expense.expenseItems.first().assignments.first().amount)
    }

    @Test
    fun `leaveTrip deletes wishlist entries and marks member exited`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.memberUser.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)

        service.leaveTrip(TripCommand.Leave(fixture.trip.id))

        assertEquals(TripRole.EXITED, fixture.member.role)
        verify(wishlistItemRepository).deleteByAdderId(fixture.member.id)
    }

    @Test
    fun `kickMember deletes wishlist entries and marks member kicked`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.ownerUser.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(tripAuthorizationPolicy.isTripAdmin(fixture.trip.id)).thenReturn(true)

        service.kickMember(TripCommand.KickMember(fixture.trip.id, fixture.memberUser.id))

        assertEquals(TripRole.KICKED, fixture.member.role)
        verify(wishlistItemRepository).deleteByAdderId(fixture.member.id)
    }

    @Test
    fun `leaveTrip rejects owner`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.ownerUser.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service.leaveTrip(TripCommand.Leave(fixture.trip.id))
        }

        assertEquals(ErrorCode.NO_AUTHORITY_TRIP, ex.errorCode)
    }

    private fun fixture(): Fixture {
        val trip = Trip("Trip", LocalDate.of(2026, 4, 12), LocalDate.of(2026, 4, 13), Country.JAPAN)
        ReflectionTestUtils.setField(trip, "id", 5L)
        val ownerUser = Member(id = 1L, email = "owner@test.com", passwordHash = "pw", nickname = "owner")
        val memberUser = Member(id = 2L, email = "member@test.com", passwordHash = "pw", nickname = "member")
        val owner = TripMember(ownerUser, trip, role = TripRole.OWNER)
        val member = TripMember(memberUser, trip, role = TripRole.MEMBER)
        val guest = TripMember(null, trip, guestNickname = "guest", role = TripRole.GUEST)
        ReflectionTestUtils.setField(owner, "id", 11L)
        ReflectionTestUtils.setField(member, "id", 12L)
        ReflectionTestUtils.setField(guest, "id", 13L)
        trip.members.add(owner)
        trip.members.add(member)
        trip.members.add(guest)

        val place = Place("place", "place", "addr", BigDecimal.ZERO, BigDecimal.ZERO)
        ReflectionTestUtils.setField(place, "id", 31L)
        val wishlist = WishlistItem(trip, place, guest)
        ReflectionTestUtils.setField(wishlist, "id", 41L)
        trip.wishlistItems.add(wishlist)

        return Fixture(trip, ownerUser, memberUser, owner, member, guest)
    }

    private fun guestExpense(fixture: Fixture, withRemainingMembers: Boolean): Expense {
        val expense = Expense(
            trip = fixture.trip,
            createdBy = fixture.ownerUser,
            payer = fixture.guest,
            title = "Shared",
            amount = BigDecimal("10000"),
            currencyCode = "KRW",
            spentAt = LocalDate.of(2026, 4, 12),
            category = ExpenseCategory.FOOD,
            splitType = ExpenseSplitType.EQUAL,
        )
        ReflectionTestUtils.setField(expense, "id", 51L)
        val item = ExpenseItem(expense, "Item", BigDecimal("10000"))
        ReflectionTestUtils.setField(item, "id", 61L)
        item.assignments.add(ExpenseAssignment(item, fixture.guest, BigDecimal("5000")))
        if (withRemainingMembers) {
            item.assignments.add(ExpenseAssignment(item, fixture.owner, BigDecimal("2500")))
            item.assignments.add(ExpenseAssignment(item, fixture.member, BigDecimal("2500")))
        }
        expense.expenseItems.add(item)
        return expense
    }

    private data class Fixture(
        val trip: Trip,
        val ownerUser: Member,
        val memberUser: Member,
        val owner: TripMember,
        val member: TripMember,
        val guest: TripMember,
    )
}
