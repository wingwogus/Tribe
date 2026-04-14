package com.tribe.application.itinerary.item

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchResult
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.core.TripRepository
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
    @Mock private lateinit var itineraryItemRepository: ItineraryItemRepository
    @Mock private lateinit var placeRepository: PlaceRepository
    @Mock private lateinit var placeSearchService: PlaceSearchService
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripRealtimeEventPublisher: TripRealtimeEventPublisher

    private lateinit var itemService: ItemService

    @BeforeEach
    fun setUp() {
        itemService = ItemService(
            itineraryItemRepository = itineraryItemRepository,
            placeRepository = placeRepository,
            placeSearchService = placeSearchService,
            currentActor = currentActor,
            tripRealtimeEventPublisher = tripRealtimeEventPublisher,
            tripAuthorizationPolicy = com.tribe.application.trip.core.TripAuthorizationPolicy(tripMemberRepository, currentActor),
            tripRepository = tripRepository,
        )
    }

    @Test
    fun `createItem appends next order in visit day`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(itineraryItemRepository.countByTripIdAndVisitDay(fixture.trip.id, 1)).thenReturn(2)
        `when`(itineraryItemRepository.save(any(ItineraryItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as ItineraryItem
            ReflectionTestUtils.setField(saved, "id", 77L)
            saved
        }

        val result = itemService.createItem(
            ItemCommand.Create(
                tripId = fixture.trip.id,
                visitDay = 1,
                title = "Dinner",
                time = LocalDateTime.of(2026, 4, 12, 19, 0),
                memo = "Booked",
            ),
        )

        assertEquals(77L, result.itemId)
        assertEquals(3, result.itemOrder)
        assertEquals(1, result.visitDay)
        assertEquals("Dinner", result.title)
    }

    @Test
    fun `createItem rejects missing trip`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.empty())

        val ex = assertThrows(BusinessException::class.java) {
            itemService.createItem(
                ItemCommand.Create(
                    tripId = fixture.trip.id,
                    visitDay = 1,
                    title = "Dinner",
                ),
            )
        }

        assertEquals(ErrorCode.TRIP_NOT_FOUND, ex.errorCode)
        verify(itineraryItemRepository, never()).save(any(ItineraryItem::class.java))
    }

    @Test
    fun `updateItem moves item to requested visit day`() {
        val fixture = fixture()
        val item = ItineraryItem(
            trip = fixture.trip,
            visitDay = 1,
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
        `when`(itineraryItemRepository.countByTripIdAndVisitDay(fixture.trip.id, 2)).thenReturn(4)

        val result = itemService.updateItem(
            ItemCommand.Update(
                tripId = fixture.trip.id,
                itemId = 55L,
                visitDay = 2,
                title = "Late Lunch",
            ),
        )

        assertEquals(2, result.visitDay)
        assertEquals(5, result.itemOrder)
        assertEquals("Late Lunch", result.title)
    }

    @Test
    fun `updateItemOrder moves items across days and order`() {
        val fixture = fixture()
        val item = ItineraryItem(fixture.trip, 1, null, "Lunch", null, 1, null)
        ReflectionTestUtils.setField(item, "id", 55L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findByIdInAndTripId(listOf(55L), fixture.trip.id)).thenReturn(listOf(item))

        val result = itemService.updateItemOrder(
            ItemCommand.OrderUpdate(
                tripId = fixture.trip.id,
                items = listOf(ItemCommand.OrderItem(55L, 2, 3)),
            ),
        )

        assertEquals(2, result.first().visitDay)
        assertEquals(3, result.first().itemOrder)
    }

    @Test
    fun `getAllDirectionsForTrip returns routes for adjacent place based items`() {
        val fixture = fixture()
        val originPlace = Place("origin", "Origin", "addr1", java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)
        val destinationPlace = Place("dest", "Destination", "addr2", java.math.BigDecimal.ONE, java.math.BigDecimal.ONE)
        val originItem = ItineraryItem(fixture.trip, 1, originPlace, null, null, 1, null)
        val destinationItem = ItineraryItem(fixture.trip, 1, destinationPlace, null, null, 2, null)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findByTripIdOrderByVisitDayAndOrder(fixture.trip.id)).thenReturn(listOf(originItem, destinationItem))
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
        return Fixture(trip, member, tripMember)
    }

    private data class Fixture(
        val trip: Trip,
        val member: Member,
        val tripMember: com.tribe.domain.trip.member.TripMember,
    )
}
