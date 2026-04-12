package com.tribe.application.itinerary.item

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchResult
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.category.CategoryRepository
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.member.TripRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ItemServiceTest {
    @Mock private lateinit var categoryRepository: CategoryRepository
    @Mock private lateinit var itineraryItemRepository: ItineraryItemRepository
    @Mock private lateinit var placeRepository: PlaceRepository
    @Mock private lateinit var placeSearchService: PlaceSearchService
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripRealtimeEventPublisher: TripRealtimeEventPublisher

    private lateinit var itemService: ItemService

    @BeforeEach
    fun setUp() {
        itemService = ItemService(
            categoryRepository = categoryRepository,
            itineraryItemRepository = itineraryItemRepository,
            placeRepository = placeRepository,
            placeSearchService = placeSearchService,
            currentActor = currentActor,
            tripRealtimeEventPublisher = tripRealtimeEventPublisher,
            tripAuthorizationPolicy = com.tribe.application.trip.core.TripAuthorizationPolicy(tripMemberRepository, currentActor),
        )
    }

    @Test
    fun `createItem appends next order in category`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(categoryRepository.findById(fixture.category.id)).thenReturn(Optional.of(fixture.category))
        `when`(itineraryItemRepository.countByCategoryId(fixture.category.id)).thenReturn(2)
        `when`(itineraryItemRepository.save(any(ItineraryItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as ItineraryItem
            ReflectionTestUtils.setField(saved, "id", 77L)
            saved
        }

        val result = itemService.createItem(
            ItemCommand.Create(
                tripId = fixture.trip.id,
                categoryId = fixture.category.id,
                title = "Dinner",
                time = LocalDateTime.of(2026, 4, 12, 19, 0),
                memo = "Booked",
            ),
        )

        assertEquals(77L, result.itemId)
        assertEquals(3, result.order)
        assertEquals("Dinner", result.title)
    }

    @Test
    fun `createItem rejects category from another trip`() {
        val fixture = fixture()
        val otherTrip = Trip(
            title = "Other",
            startDate = LocalDate.of(2026, 4, 15),
            endDate = LocalDate.of(2026, 4, 17),
            country = Country.SOUTH_KOREA,
        )
        ReflectionTestUtils.setField(otherTrip, "id", 30L)
        val foreignCategory = Category(otherTrip, 1, "Foreign", 1)
        ReflectionTestUtils.setField(foreignCategory, "id", 99L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(categoryRepository.findById(99L)).thenReturn(Optional.of(foreignCategory))

        val ex = assertThrows(BusinessException::class.java) {
            itemService.createItem(
                ItemCommand.Create(
                    tripId = fixture.trip.id,
                    categoryId = 99L,
                    title = "Dinner",
                ),
            )
        }

        assertEquals(ErrorCode.NO_BELONG_TRIP, ex.errorCode)
        verify(itineraryItemRepository, never()).save(any(ItineraryItem::class.java))
    }

    @Test
    fun `updateItem moves item to requested category`() {
        val fixture = fixture()
        val secondCategory = Category(fixture.trip, 2, "Evening", 2)
        ReflectionTestUtils.setField(secondCategory, "id", 12L)
        val item = ItineraryItem(
            category = fixture.category,
            place = null,
            title = "Lunch",
            time = LocalDateTime.of(2026, 4, 12, 13, 0),
            order = 1,
            memo = null,
        )
        ReflectionTestUtils.setField(item, "id", 55L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findById(55L)).thenReturn(Optional.of(item))
        `when`(categoryRepository.findById(12L)).thenReturn(Optional.of(secondCategory))
        `when`(itineraryItemRepository.countByCategoryId(12L)).thenReturn(4)

        val result = itemService.updateItem(
            ItemCommand.Update(
                tripId = fixture.trip.id,
                itemId = 55L,
                categoryId = 12L,
                title = "Late Lunch",
            ),
        )

        assertEquals(12L, result.categoryId)
        assertEquals(5, result.order)
        assertEquals("Late Lunch", result.title)
    }

    @Test
    fun `updateItemOrder moves items across categories and order`() {
        val fixture = fixture()
        val secondCategory = Category(fixture.trip, 1, "Evening", 2)
        ReflectionTestUtils.setField(secondCategory, "id", 12L)
        val item = ItineraryItem(fixture.category, null, "Lunch", null, 1, null)
        ReflectionTestUtils.setField(item, "id", 55L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findByIdInAndTripId(listOf(55L), fixture.trip.id)).thenReturn(listOf(item))
        `when`(categoryRepository.findAllById(listOf(12L))).thenReturn(listOf(secondCategory))

        val result = itemService.updateItemOrder(
            ItemCommand.OrderUpdate(
                tripId = fixture.trip.id,
                items = listOf(ItemCommand.OrderItem(55L, 12L, 3)),
            ),
        )

        assertEquals(12L, result.first().categoryId)
        assertEquals(3, result.first().order)
    }

    @Test
    fun `getAllDirectionsForTrip returns routes for adjacent place based items`() {
        val fixture = fixture()
        val originPlace = Place("origin", "Origin", "addr1", java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)
        val destinationPlace = Place("dest", "Destination", "addr2", java.math.BigDecimal.ONE, java.math.BigDecimal.ONE)
        val originItem = ItineraryItem(fixture.category, originPlace, null, null, 1, null)
        val destinationItem = ItineraryItem(fixture.category, destinationPlace, null, null, 2, null)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findByTripIdOrderByCategoryAndOrder(fixture.trip.id)).thenReturn(listOf(originItem, destinationItem))
        `when`(placeSearchService.directions("origin", "dest", "walking")).thenReturn(
            RouteDetails(
                travelMode = "WALKING",
                originPlace = PlaceSearchResult("origin", "Origin", "addr1", 0.0, 0.0),
                destinationPlace = PlaceSearchResult("dest", "Destination", "addr2", 1.0, 1.0),
                totalDuration = "10 mins",
                totalDistance = "1 km",
                steps = emptyList(),
            ),
        )

        val result = itemService.getAllDirectionsForTrip(fixture.trip.id, "walking")

        assertEquals(1, result.size)
        assertEquals("WALKING", result.first().travelMode)
    }

    private fun fixture(): Fixture {
        val trip = Trip(
            title = "Tokyo",
            startDate = LocalDate.of(2026, 4, 10),
            endDate = LocalDate.of(2026, 4, 14),
            country = Country.JAPAN,
        )
        ReflectionTestUtils.setField(trip, "id", 1L)
        val member = Member(id = 2L, email = "member@example.com", passwordHash = "hashed", nickname = "member")
        val tripMember = com.tribe.domain.trip.member.TripMember(member = member, trip = trip, role = TripRole.MEMBER)
        ReflectionTestUtils.setField(tripMember, "id", 3L)
        val category = Category(trip, 1, "Meals", 1)
        ReflectionTestUtils.setField(category, "id", 11L)
        return Fixture(trip, member, tripMember, category)
    }

    private data class Fixture(
        val trip: Trip,
        val member: Member,
        val tripMember: com.tribe.domain.trip.member.TripMember,
        val category: Category,
    )
}
