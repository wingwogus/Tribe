package com.tribe.application.itinerary.wishlist

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceCatalogService
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.event.TripRealtimeEventType
import com.tribe.application.trip.event.WishlistAction
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.wishlist.MemberWishlistItem
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class WishlistServiceTest {
    @Mock private lateinit var wishlistItemRepository: WishlistItemRepository
    @Mock private lateinit var memberWishlistItemRepository: MemberWishlistItemRepository
    @Mock private lateinit var placeCatalogService: PlaceCatalogService
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripAuthorizationPolicy: com.tribe.application.trip.core.TripAuthorizationPolicy
    private lateinit var tripRealtimeEventPublisher: RecordingTripRealtimeEventPublisher

    private lateinit var service: WishlistService

    @BeforeEach
    fun setUp() {
        tripRealtimeEventPublisher = RecordingTripRealtimeEventPublisher()
        service = WishlistService(
            wishlistItemRepository = wishlistItemRepository,
            memberWishlistItemRepository = memberWishlistItemRepository,
            placeCatalogService = placeCatalogService,
            tripMemberRepository = tripMemberRepository,
            tripRepository = tripRepository,
            memberRepository = memberRepository,
            currentActor = currentActor,
            tripRealtimeEventPublisher = tripRealtimeEventPublisher,
            tripAuthorizationPolicy = tripAuthorizationPolicy,
        )
    }

    @Test
    fun `addWishList creates place when missing`() {
        val fixture = fixture()
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(memberRepository.findById(fixture.member.id)).thenReturn(Optional.of(fixture.member))
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(tripMemberRepository.findByTripAndMember(fixture.trip, fixture.member)).thenReturn(fixture.tripMember)
        `when`(wishlistItemRepository.existsByTrip_IdAndPlace_ExternalPlaceId(fixture.trip.id, "new_place")).thenReturn(false)
        val place = Place("new_place", "도쿄타워", "도쿄", BigDecimal.ZERO, BigDecimal.ZERO)
        ReflectionTestUtils.setField(place, "id", 50L)
        `when`(
            placeCatalogService.getOrCreateAndEnrich(
                "new_place",
                "도쿄타워",
                "도쿄",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "ko",
            ),
        ).thenReturn(place)
        `when`(wishlistItemRepository.save(any(WishlistItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as WishlistItem
            ReflectionTestUtils.setField(saved, "id", 60L)
            saved
        }

        val result = service.addWishList(
            WishlistCommand.Add(
                tripId = fixture.trip.id,
                externalPlaceId = "new_place",
                placeName = "도쿄타워",
                address = "도쿄",
                latitude = BigDecimal.ZERO,
                longitude = BigDecimal.ZERO,
            ),
        )

        assertEquals(60L, result.wishlistItemId)
        assertEquals("도쿄타워", result.name)
    }

    @Test
    fun `addWishList rejects duplicate place in trip`() {
        val fixture = fixture()
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(memberRepository.findById(fixture.member.id)).thenReturn(Optional.of(fixture.member))
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(tripMemberRepository.findByTripAndMember(fixture.trip, fixture.member)).thenReturn(fixture.tripMember)
        `when`(wishlistItemRepository.existsByTrip_IdAndPlace_ExternalPlaceId(fixture.trip.id, "existing")).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service.addWishList(
                WishlistCommand.Add(
                    tripId = fixture.trip.id,
                    externalPlaceId = "existing",
                    placeName = "오사카성",
                    address = "오사카",
                    latitude = BigDecimal.ONE,
                    longitude = BigDecimal.ONE,
                ),
            )
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS, ex.errorCode)
    }

    @Test
    fun `addWishListFromMemberWishlist creates trip wishlist from owned source`() {
        val fixture = fixture()
        val sourcePlace = place("member-wishlist-place", "도쿄타워")
        val sourceItem = memberWishlistItem(fixture.member, sourcePlace, 99L)
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(memberRepository.findById(fixture.member.id)).thenReturn(Optional.of(fixture.member))
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(tripMemberRepository.findByTripAndMember(fixture.trip, fixture.member)).thenReturn(fixture.tripMember)
        `when`(memberWishlistItemRepository.findByIdAndMemberIdWithPlace(99L, fixture.member.id)).thenReturn(sourceItem)
        `when`(wishlistItemRepository.existsByTrip_IdAndPlace_ExternalPlaceId(fixture.trip.id, "member-wishlist-place"))
            .thenReturn(false)
        `when`(wishlistItemRepository.save(any(WishlistItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as WishlistItem
            ReflectionTestUtils.setField(saved, "id", 70L)
            saved
        }

        val result = service.addWishListFromMemberWishlist(
            WishlistCommand.AddFromMemberWishlist(
                tripId = fixture.trip.id,
                memberWishlistItemId = 99L,
            ),
        )

        assertEquals(70L, result.wishlistItemId)
        assertEquals(sourcePlace.id, result.placeId)
        assertEquals("도쿄타워", result.name)
        val event = tripRealtimeEventPublisher.events.single()
        assertEquals(TripRealtimeEventType.WISHLIST, event.type)
        assertEquals(fixture.trip.id, event.tripId)
        assertEquals(fixture.member.id, event.actorId)
        assertEquals(WishlistAction.ADDED, event.wishlist?.action)
        assertEquals(70L, event.wishlist?.item?.wishlistItemId)
    }

    @Test
    fun `addWishListFromMemberWishlist rejects missing or foreign source`() {
        val fixture = fixture()
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(memberRepository.findById(fixture.member.id)).thenReturn(Optional.of(fixture.member))
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(tripMemberRepository.findByTripAndMember(fixture.trip, fixture.member)).thenReturn(fixture.tripMember)
        `when`(memberWishlistItemRepository.findByIdAndMemberIdWithPlace(99L, fixture.member.id)).thenReturn(null)

        val ex = assertThrows(BusinessException::class.java) {
            service.addWishListFromMemberWishlist(
                WishlistCommand.AddFromMemberWishlist(
                    tripId = fixture.trip.id,
                    memberWishlistItemId = 99L,
                ),
            )
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_NOT_FOUND, ex.errorCode)
        verify(wishlistItemRepository, never()).save(any(WishlistItem::class.java))
    }

    @Test
    fun `addWishListFromMemberWishlist keeps duplicate trip wishlist rule`() {
        val fixture = fixture()
        val sourcePlace = place("existing", "오사카성")
        val sourceItem = memberWishlistItem(fixture.member, sourcePlace, 99L)
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)
        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(memberRepository.findById(fixture.member.id)).thenReturn(Optional.of(fixture.member))
        `when`(tripRepository.findById(fixture.trip.id)).thenReturn(Optional.of(fixture.trip))
        `when`(tripMemberRepository.findByTripAndMember(fixture.trip, fixture.member)).thenReturn(fixture.tripMember)
        `when`(memberWishlistItemRepository.findByIdAndMemberIdWithPlace(99L, fixture.member.id)).thenReturn(sourceItem)
        `when`(wishlistItemRepository.existsByTrip_IdAndPlace_ExternalPlaceId(fixture.trip.id, "existing"))
            .thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service.addWishListFromMemberWishlist(
                WishlistCommand.AddFromMemberWishlist(
                    tripId = fixture.trip.id,
                    memberWishlistItemId = 99L,
                ),
            )
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS, ex.errorCode)
        verify(wishlistItemRepository, never()).save(any(WishlistItem::class.java))
    }

    @Test
    fun `addWishListFromMemberWishlist rejects non trip member before source lookup`() {
        val fixture = fixture()
        doThrow(BusinessException(ErrorCode.NOT_A_TRIP_MEMBER))
            .`when`(tripAuthorizationPolicy).isTripMember(fixture.trip.id)

        val ex = assertThrows(BusinessException::class.java) {
            service.addWishListFromMemberWishlist(
                WishlistCommand.AddFromMemberWishlist(
                    tripId = fixture.trip.id,
                    memberWishlistItemId = 99L,
                ),
            )
        }

        assertEquals(ErrorCode.NOT_A_TRIP_MEMBER, ex.errorCode)
        verify(memberWishlistItemRepository, never()).findByIdAndMemberIdWithPlace(99L, fixture.member.id)
        verify(wishlistItemRepository, never()).save(any(WishlistItem::class.java))
    }

    @Test
    fun `searchWishList returns empty page when nothing matches`() {
        val fixture = fixture()
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)
        `when`(wishlistItemRepository.findAllByTrip_IdAndPlace_NameContainingIgnoreCase(fixture.trip.id, "도쿄", PageRequest.of(0, 10)))
            .thenReturn(PageImpl(emptyList(), PageRequest.of(0, 10), 0))

        val result = service.searchWishList(fixture.trip.id, "도쿄", PageRequest.of(0, 10))

        assertEquals(0, result.totalElements)
        assertEquals(true, result.content.isEmpty())
    }

    @Test
    fun `deleteWishlistItems rejects missing ids`() {
        val fixture = fixture()
        `when`(tripAuthorizationPolicy.isTripMember(fixture.trip.id)).thenReturn(true)
        `when`(wishlistItemRepository.findIdsByTripIdAndIdIn(fixture.trip.id, listOf(1L, 2L))).thenReturn(listOf(1L))

        val ex = assertThrows(BusinessException::class.java) {
            service.deleteWishlistItems(WishlistCommand.Delete(fixture.trip.id, listOf(1L, 2L)))
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_NOT_FOUND, ex.errorCode)
    }

    private fun fixture(): Fixture {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(2), Country.JAPAN)
        ReflectionTestUtils.setField(trip, "id", 5L)
        val member = Member(id = 2L, email = "member@test.com", passwordHash = "pw", nickname = "member")
        val tripMember = TripMember(member, trip, role = TripRole.MEMBER)
        ReflectionTestUtils.setField(tripMember, "id", 3L)
        return Fixture(trip, member, tripMember)
    }

    private fun place(externalPlaceId: String, name: String): Place {
        val place = Place(externalPlaceId, name, "도쿄", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 80L)
        return place
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
        val tripMember: TripMember,
    )
}
