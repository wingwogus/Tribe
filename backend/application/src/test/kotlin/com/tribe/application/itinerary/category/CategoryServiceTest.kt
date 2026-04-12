package com.tribe.application.itinerary.category

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.category.CategoryRepository
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
import org.mockito.Mockito.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryServiceTest {
    @Mock private lateinit var categoryRepository: CategoryRepository
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var tripMemberRepository: com.tribe.domain.trip.member.TripMemberRepository
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripRealtimeEventPublisher: TripRealtimeEventPublisher

    private lateinit var service: CategoryService

    @BeforeEach
    fun setUp() {
        service = CategoryService(
            categoryRepository = categoryRepository,
            tripRepository = tripRepository,
            currentActor = currentActor,
            tripRealtimeEventPublisher = tripRealtimeEventPublisher,
            tripAuthorizationPolicy = com.tribe.application.trip.core.TripAuthorizationPolicy(tripMemberRepository, currentActor),
        )
    }

    @Test
    fun `createCategory saves new category`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(categoryRepository.save(any(Category::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Category
            ReflectionTestUtils.setField(saved, "id", 20L)
            saved
        }

        val result = service.createCategory(CategoryCommand.Create(fixture.trip.id, "새 카테고리", 3, 1))

        assertEquals(20L, result.categoryId)
        assertEquals("새 카테고리", result.name)
        assertEquals(3, result.day)
    }

    @Test
    fun `orderUpdateCategory rejects duplicate category ids`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)

        val ex = assertThrows(BusinessException::class.java) {
            service.orderUpdateCategory(
                CategoryCommand.OrderUpdate(
                    tripId = fixture.trip.id,
                    items = listOf(
                        CategoryCommand.OrderItem(11L, 1),
                        CategoryCommand.OrderItem(11L, 2),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.DUPLICATE_CATEGORY_ID_REQUEST, ex.errorCode)
    }

    @Test
    fun `orderUpdateCategory rejects categories from different day`() {
        val fixture = fixture()
        val second = Category(fixture.trip, 2, "Day 2", 1)
        ReflectionTestUtils.setField(second, "id", 12L)
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(categoryRepository.findAllById(anyList())).thenReturn(listOf(fixture.category, second))

        val ex = assertThrows(BusinessException::class.java) {
            service.orderUpdateCategory(
                CategoryCommand.OrderUpdate(
                    tripId = fixture.trip.id,
                    items = listOf(
                        CategoryCommand.OrderItem(11L, 2),
                        CategoryCommand.OrderItem(12L, 1),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `deleteCategory rejects foreign trip category`() {
        val fixture = fixture()
        val otherTrip = Trip("Other", LocalDate.now(), LocalDate.now().plusDays(1), Country.SOUTH_KOREA)
        ReflectionTestUtils.setField(otherTrip, "id", 99L)
        val foreign = Category(otherTrip, 1, "Foreign", 1)
        ReflectionTestUtils.setField(foreign, "id", 77L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(categoryRepository.findById(77L)).thenReturn(Optional.of(foreign))

        val ex = assertThrows(BusinessException::class.java) {
            service.deleteCategory(fixture.trip.id, 77L)
        }

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.errorCode)
        verify(categoryRepository, never()).delete(any(Category::class.java))
    }

    private fun fixture(): Fixture {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(2), Country.JAPAN)
        ReflectionTestUtils.setField(trip, "id", 5L)
        val member = Member(id = 2L, email = "member@test.com", passwordHash = "pw", nickname = "member")
        val tripMember = TripMember(member, trip, role = TripRole.MEMBER)
        ReflectionTestUtils.setField(tripMember, "id", 3L)
        val category = Category(trip, 1, "Day 1", 1)
        ReflectionTestUtils.setField(category, "id", 11L)
        return Fixture(trip, member, tripMember, category)
    }

    private data class Fixture(
        val trip: Trip,
        val member: Member,
        val tripMember: TripMember,
        val category: Category,
    )
}
