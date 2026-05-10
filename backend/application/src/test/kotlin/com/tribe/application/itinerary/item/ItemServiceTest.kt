package com.tribe.application.itinerary.item

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.OpeningHoursEvaluator
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.itinerary.place.PlaceSearchGateway
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.ItineraryAction
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.itinerary.wishlist.MemberWishlistItem
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
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
    @Mock private lateinit var memberWishlistItemRepository: MemberWishlistItemRepository
    @Mock private lateinit var placeSearchService: PlaceSearchService
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var placeResultAssembler: PlaceResultAssembler
    @Mock private lateinit var openingHoursEvaluator: OpeningHoursEvaluator
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var currentActor: CurrentActor
    private lateinit var tripRealtimeEventPublisher: RecordingTripRealtimeEventPublisher

    private lateinit var itemService: ItemService

    @BeforeEach
    fun setUp() {
        tripRealtimeEventPublisher = RecordingTripRealtimeEventPublisher()
        itemService = ItemService(
            itineraryItemRepository = itineraryItemRepository,
            placeRepository = placeRepository,
            memberWishlistItemRepository = memberWishlistItemRepository,
            placeSearchService = placeSearchService,
            placeResultAssembler = placeResultAssembler,
            openingHoursEvaluator = openingHoursEvaluator,
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
    fun `createItem creates place based item from place id`() {
        val fixture = fixture()
        val place = Place("account-wishlist-place", "도쿄타워", "도쿄", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 90L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(placeRepository.findById(90L)).thenReturn(Optional.of(place))
        `when`(itineraryItemRepository.countByTripIdAndVisitDay(fixture.trip.id, 1)).thenReturn(0)
        `when`(itineraryItemRepository.save(any(ItineraryItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as ItineraryItem
            ReflectionTestUtils.setField(saved, "id", 91L)
            saved
        }

        val result = itemService.createItem(
            ItemCommand.Create(
                tripId = fixture.trip.id,
                visitDay = 1,
                placeId = 90L,
            ),
        )

        assertEquals(91L, result.itemId)
        assertEquals(90L, result.placeId)
        assertEquals("account-wishlist-place", result.externalPlaceId)
        assertEquals("도쿄타워", result.name)
    }

    @Test
    fun `createItemFromMemberWishlist creates place based item from owned source`() {
        val fixture = fixture()
        val place = Place("member-wishlist-place", "도쿄타워", "도쿄", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 90L)
        val sourceItem = memberWishlistItem(fixture.member, place, 30L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(memberWishlistItemRepository.findByIdAndMemberIdWithPlace(30L, fixture.member.id)).thenReturn(sourceItem)
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(itineraryItemRepository.countByTripIdAndVisitDay(fixture.trip.id, 2)).thenReturn(4)
        `when`(itineraryItemRepository.save(any(ItineraryItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as ItineraryItem
            ReflectionTestUtils.setField(saved, "id", 92L)
            saved
        }

        val result = itemService.createItemFromMemberWishlist(
            ItemCommand.CreateFromMemberWishlist(
                tripId = fixture.trip.id,
                memberWishlistItemId = 30L,
                visitDay = 2,
                time = LocalDateTime.of(2026, 4, 12, 19, 0),
                memo = "야경 시간",
            ),
        )

        assertEquals(92L, result.itemId)
        assertEquals(2, result.visitDay)
        assertEquals(5, result.itemOrder)
        assertEquals(90L, result.placeId)
        assertEquals("member-wishlist-place", result.externalPlaceId)
        assertEquals("야경 시간", result.memo)
        val event = tripRealtimeEventPublisher.events.single()
        assertEquals(TripRealtimeEventType.ITINERARY, event.type)
        assertEquals(fixture.trip.id, event.tripId)
        assertEquals(fixture.member.id, event.actorId)
        assertEquals(ItineraryAction.ITEM_CREATED, event.itinerary?.action)
        assertEquals(92L, event.itinerary?.item?.itemId)
    }

    @Test
    fun `createItemFromMemberWishlist rejects missing or foreign source`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(memberWishlistItemRepository.findByIdAndMemberIdWithPlace(30L, fixture.member.id)).thenReturn(null)

        val ex = assertThrows(BusinessException::class.java) {
            itemService.createItemFromMemberWishlist(
                ItemCommand.CreateFromMemberWishlist(
                    tripId = fixture.trip.id,
                    memberWishlistItemId = 30L,
                    visitDay = 2,
                ),
            )
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_NOT_FOUND, ex.errorCode)
        verify(itineraryItemRepository, never()).save(any(ItineraryItem::class.java))
    }

    @Test
    fun `createItemFromMemberWishlist rejects non trip member before source lookup`() {
        val fixture = fixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)

        val ex = assertThrows(BusinessException::class.java) {
            itemService.createItemFromMemberWishlist(
                ItemCommand.CreateFromMemberWishlist(
                    tripId = fixture.trip.id,
                    memberWishlistItemId = 30L,
                    visitDay = 2,
                ),
            )
        }

        assertEquals(ErrorCode.NOT_A_TRIP_MEMBER, ex.errorCode)
        verify(memberWishlistItemRepository, never()).findByIdAndMemberIdWithPlace(30L, fixture.member.id)
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
    fun `updateItem clears time and memo when null is provided`() {
        val fixture = fixture()
        val item = ItineraryItem(
            trip = fixture.trip,
            visitDay = 1,
            place = null,
            title = "Lunch",
            time = LocalDateTime.of(2026, 4, 12, 13, 0),
            order = 1,
            memo = "Window seat",
        )
        ReflectionTestUtils.setField(item, "id", 55L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findById(55L)).thenReturn(Optional.of(item))

        val result = itemService.updateItem(
            ItemCommand.Update(
                tripId = fixture.trip.id,
                itemId = 55L,
                time = null,
                memo = null,
            ),
        )

        assertEquals(null, result.time)
        assertEquals(null, result.memo)
    }

    @Test
    fun `getItem passes through null opening status`() {
        val fixture = fixture()
        val place = Place("place-1", "Cafe", "addr", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN)
        val item = ItineraryItem(fixture.trip, 1, place, null, null, 1, null)
        ReflectionTestUtils.setField(item, "id", 55L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findById(55L)).thenReturn(Optional.of(item))
        `when`(openingHoursEvaluator.evaluate(item, fixture.trip.startDate)).thenReturn(null)

        val result = itemService.getItem(fixture.trip.id, 55L)

        assertEquals(null, result.openingStatusWarning)
    }

    @Test
    fun `getItem keeps visible opening status values`() {
        val fixture = fixture()
        val place = Place("place-1", "Cafe", "addr", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN)
        val item = ItineraryItem(fixture.trip, 1, place, null, null, 1, null)
        ReflectionTestUtils.setField(item, "id", 56L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripMemberRepository.findByTripIdAndMemberId(fixture.trip.id, fixture.member.id)).thenReturn(fixture.tripMember)
        `when`(itineraryItemRepository.findById(56L)).thenReturn(Optional.of(item))
        `when`(openingHoursEvaluator.evaluate(item, fixture.trip.startDate)).thenReturn("CLOSED_DAY_POSSIBLE")

        val result = itemService.getItem(fixture.trip.id, 56L)

        assertEquals("CLOSED_DAY_POSSIBLE", result.openingStatusWarning)
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
                originPlace = PlaceSearchGateway.SearchHit(
                    externalPlaceId = "origin",
                    placeName = "Origin",
                    address = "addr1",
                    latitude = 0.0,
                    longitude = 0.0,
                ),
                destinationPlace = PlaceSearchGateway.SearchHit(
                    externalPlaceId = "dest",
                    placeName = "Destination",
                    address = "addr2",
                    latitude = 1.0,
                    longitude = 1.0,
                ),
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

    private fun memberWishlistItem(member: Member, place: Place, id: Long): MemberWishlistItem {
        val item = MemberWishlistItem(member, place)
        ReflectionTestUtils.setField(item, "id", id)
        return item
    }

    private class RecordingTripRealtimeEventPublisher : TripRealtimeEventPublisher {
        val events = mutableListOf<TripRealtimeEvent>()

        override fun publish(event: TripRealtimeEvent) {
            events.add(event)
        }
    }

    private data class Fixture(
        val trip: Trip,
        val member: Member,
        val tripMember: com.tribe.domain.trip.member.TripMember,
    )
}
