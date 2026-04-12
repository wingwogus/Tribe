package com.tribe.application.itinerary.wishlist

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
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
    @Mock private lateinit var placeRepository: PlaceRepository
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripAuthorizationPolicy: com.tribe.application.trip.core.TripAuthorizationPolicy
    @Mock private lateinit var tripRealtimeEventPublisher: TripRealtimeEventPublisher

    private lateinit var service: WishlistService

    @BeforeEach
    fun setUp() {
        service = WishlistService(
            wishlistItemRepository = wishlistItemRepository,
            placeRepository = placeRepository,
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
        `when`(placeRepository.findByExternalPlaceId("new_place")).thenReturn(null)
        `when`(placeRepository.save(any(Place::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Place
            ReflectionTestUtils.setField(saved, "id", 50L)
            saved
        }
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

    private data class Fixture(
        val trip: Trip,
        val member: Member,
        val tripMember: TripMember,
    )
}
