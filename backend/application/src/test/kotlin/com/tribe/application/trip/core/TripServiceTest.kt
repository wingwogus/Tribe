package com.tribe.application.trip.core

import com.tribe.application.redis.TripInvitationRepository
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.member.TripMemberIntegrityService
import com.tribe.domain.community.CommunityPost
import com.tribe.domain.community.CommunityPostRepository
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.item.ItineraryItem
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
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class TripServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripAuthorizationPolicy: TripAuthorizationPolicy
    @Mock private lateinit var tripMemberIntegrityService: TripMemberIntegrityService
    @Mock private lateinit var tripRealtimeEventPublisher: TripRealtimeEventPublisher
    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var tripInvitationRepository: TripInvitationRepository
    @Mock private lateinit var communityPostRepository: CommunityPostRepository

    private lateinit var tripService: TripService

    @BeforeEach
    fun setUp() {
        tripService = TripService(
            currentActor = currentActor,
            tripAuthorizationPolicy = tripAuthorizationPolicy,
            tripMemberIntegrityService = tripMemberIntegrityService,
            tripRealtimeEventPublisher = tripRealtimeEventPublisher,
            memberRepository = memberRepository,
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository,
            tripInvitationRepository = tripInvitationRepository,
            communityPostRepository = communityPostRepository,
            appUrl = "http://localhost:3000",
        )
    }

    @Test
    fun `createTrip adds owner membership`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))
        `when`(tripRepository.save(any(Trip::class.java))).thenAnswer { it.arguments[0] as Trip }

        val result = tripService.createTrip(
            TripCommand.Create("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN.code),
        )

        assertEquals("Trip", result.title)
        assertEquals(1, result.members.size)
        assertEquals("tribe", result.members.first().nickname)
    }

    @Test
    fun `getAllTrips maps repository results`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        trip.members.add(TripMember(Member(id = 1L, email = "u@e.com", passwordHash = "p", nickname = "a"), trip, role = TripRole.OWNER))
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripRepository.findTripsByMemberId(1L, PageRequest.of(0, 10))).thenReturn(PageImpl(listOf(trip)))

        val result = tripService.getAllTrips(PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals("Trip", result.content.first().title)
    }

    @Test
    fun `joinTrip rejects kicked member`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        val tripMember = TripMember(member = member, trip = trip, role = TripRole.KICKED)

        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripInvitationRepository.getTripId("token")).thenReturn(5L)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)
        `when`(tripMemberRepository.findByTripIdAndMemberId(5L, 1L)).thenReturn(tripMember)

        val ex = assertThrows(com.tribe.application.exception.business.BusinessException::class.java) {
            tripService.joinTrip(TripCommand.Join("token"))
        }

        assertEquals(com.tribe.application.exception.ErrorCode.BANNED_MEMBER, ex.errorCode)
    }

    @Test
    fun `importTrip clones categories and itinerary items`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        val originalTrip = Trip("Original", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val category = Category(originalTrip, 1, "Day1", 1)
        val item = ItineraryItem(category, null, "Dinner", null, 1, "memo")
        category.itineraryItems.add(item)
        originalTrip.categories.add(category)
        val post = CommunityPost(member, originalTrip, "Post", "Content", null)

        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))
        `when`(communityPostRepository.findById(5L)).thenReturn(java.util.Optional.of(post))
        `when`(tripRepository.findTripWithFullItineraryById(originalTrip.id)).thenReturn(originalTrip)
        `when`(tripRepository.save(any(Trip::class.java))).thenAnswer { it.arguments[0] as Trip }

        val result = tripService.importTrip(
            TripCommand.Import(5L, "Imported", LocalDate.now(), LocalDate.now().plusDays(2)),
        )

        assertEquals("Imported", result.title)
        assertEquals(1, result.members.size)
    }

    @Test
    fun `member integrity operations delegate to integrity service`() {
        val delegated = TripResult.TripDetail(
            tripId = 5L,
            title = "Trip",
            startDate = LocalDate.of(2026, 4, 12),
            endDate = LocalDate.of(2026, 4, 13),
            country = "JP",
            members = emptyList(),
        )
        `when`(tripMemberIntegrityService.deleteGuest(TripCommand.DeleteGuest(5L, 10L))).thenReturn(delegated)
        `when`(tripMemberIntegrityService.leaveTrip(TripCommand.Leave(5L))).thenReturn(delegated)
        `when`(tripMemberIntegrityService.kickMember(TripCommand.KickMember(5L, 2L))).thenReturn(delegated)
        `when`(tripMemberIntegrityService.assignRole(TripCommand.AssignRole(5L, 2L, "admin"))).thenReturn(delegated)

        assertEquals(5L, tripService.deleteGuest(TripCommand.DeleteGuest(5L, 10L)).tripId)
        assertEquals(5L, tripService.leaveTrip(TripCommand.Leave(5L)).tripId)
        assertEquals(5L, tripService.kickMember(TripCommand.KickMember(5L, 2L)).tripId)
        assertEquals(5L, tripService.assignRole(TripCommand.AssignRole(5L, 2L, "admin")).tripId)
    }
}
