package com.tribe.api.itinerary.wishlist

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.itinerary.place.PlaceSearchCacheRepository
import com.tribe.application.itinerary.place.PlaceSearchContext
import com.tribe.application.itinerary.place.PlaceSearchGateway
import com.tribe.application.security.TokenProvider
import com.tribe.domain.itinerary.place.PlaceDetailSnapshotRepository
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriodRepository
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.itinerary.wishlist.WishlistItemLikeRepository
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.member.TripRole
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(
    properties = [
        "spring.mail.username=test@example.com",
        "spring.mail.password=test-password",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistPlaceFlowIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val tokenProvider: TokenProvider,
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val tripRepository: TripRepository,
    @Autowired private val tripMemberRepository: TripMemberRepository,
    @Autowired private val placeRepository: PlaceRepository,
    @Autowired private val detailSnapshotRepository: PlaceDetailSnapshotRepository,
    @Autowired private val openingPeriodRepository: PlaceRegularOpeningPeriodRepository,
    @Autowired private val wishlistItemRepository: WishlistItemRepository,
    @Autowired private val wishlistItemLikeRepository: WishlistItemLikeRepository,
    @Autowired private val memberWishlistItemRepository: MemberWishlistItemRepository,
) {
    @MockBean private lateinit var placeSearchGateway: PlaceSearchGateway
    @MockBean private lateinit var placeSearchCacheRepository: PlaceSearchCacheRepository

    @AfterEach
    fun cleanup() {
        wishlistItemLikeRepository.deleteAllInBatch()
        wishlistItemRepository.deleteAllInBatch()
        memberWishlistItemRepository.deleteAllInBatch()
        openingPeriodRepository.deleteAllInBatch()
        detailSnapshotRepository.deleteAllInBatch()
        tripMemberRepository.deleteAllInBatch()
        tripRepository.deleteAllInBatch()
        placeRepository.deleteAllInBatch()
        memberRepository.deleteAllInBatch()
    }

    @Test
    fun `search result can be added to trip wishlist and reused in another trip without detached place failure`() {
        val member = persistMember("flow-owner")
        val firstTrip = persistTrip("first")
        val secondTrip = persistTrip("second")
        persistTripMember(member, firstTrip, TripRole.OWNER)
        persistTripMember(member, secondTrip, TripRole.OWNER)
        val token = accessToken(member)

        `when`(placeSearchCacheRepository.get(anyString())).thenReturn(null)
        `when`(
            placeSearchGateway.search(
                "Tokyo Tower",
                "ko",
                PlaceSearchContext(regionCode = "JP"),
            ),
        ).thenReturn(
            listOf(
                PlaceSearchGateway.SearchHit(
                    externalPlaceId = "google-flow-place",
                    placeName = "Tokyo Tower",
                    address = "Tokyo",
                    latitude = 35.6586,
                    longitude = 139.7454,
                    primaryType = "tourist_attraction",
                    types = listOf("tourist_attraction", "point_of_interest"),
                ),
            ),
        )
        `when`(placeSearchGateway.getPlaceDetails("google-flow-place", "ko"))
            .thenReturn(details("google-flow-place", periodDay = 1, openMinute = 9 * 60))

        mockMvc.perform(
            get("/api/v1/places/search")
                .auth(token)
                .param("query", "Tokyo Tower")
                .param("region", "JP")
                .param("language", "ko"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].externalPlaceId", equalTo("google-flow-place")))
            .andExpect(jsonPath("$.data[0].placeTypeSummary.primaryType", equalTo("tourist_attraction")))

        val firstItem = addTripWishlist(firstTrip.id, token, "google-flow-place", "Tokyo Tower")

        val place = placeRepository.findByExternalPlaceId("google-flow-place")!!
        val snapshot = place.detailSnapshot!!
        val periods = openingPeriodRepository.findAllByPlaceIdOrderByDayOfWeekAscSequenceNoAsc(place.id)
        assertEquals(place.id, firstItem.placeId)
        assertEquals("places/google-flow-place/photos/primary", snapshot.primaryPhotoName)
        assertEquals(4.7, snapshot.rating)
        assertEquals(1, periods.size)
        assertEquals(1, periods.first().dayOfWeek)

        mockMvc.perform(
            post("/api/v1/trips/${firstTrip.id}/wishlists")
                .auth(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishlistBody("google-flow-place", "Tokyo Tower")),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code", equalTo("ITINERARY_005")))

        val secondItem = addTripWishlist(secondTrip.id, token, "google-flow-place", "Tokyo Tower")
        assertEquals(place.id, secondItem.placeId)
        assertEquals(1, placeRepository.findByExternalPlaceIdIn(listOf("google-flow-place")).size)
    }

    @Test
    fun `place detail API refreshes detail snapshot and replaces regular opening periods`() {
        val member = persistMember("refresh-owner")
        val trip = persistTrip("refresh")
        persistTripMember(member, trip, TripRole.OWNER)
        val token = accessToken(member)

        `when`(placeSearchGateway.getPlaceDetails("google-refresh-place", "ko"))
            .thenReturn(
                details("google-refresh-place", periodDay = 1, openMinute = 9 * 60),
                details("google-refresh-place", periodDay = 3, openMinute = 11 * 60, rating = 4.9, reviewCount = 99),
            )

        val item = addTripWishlist(trip.id, token, "google-refresh-place", "Refresh Place")
        val place = placeRepository.findById(item.placeId).orElseThrow()
        val staleSnapshot = detailSnapshotRepository.findById(place.id).orElseThrow()
        staleSnapshot.openingHoursSyncedAt = null
        staleSnapshot.currentOpeningHoursSyncedAt = null
        detailSnapshotRepository.saveAndFlush(staleSnapshot)

        mockMvc.perform(get("/api/v1/places/${place.id}").auth(token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.placeId", equalTo(place.id.toInt())))
            .andExpect(jsonPath("$.data.placeDetailSummary.rating", equalTo(4.9)))

        val refreshedPlace = placeRepository.findById(place.id).orElseThrow()
        val refreshedPeriods = openingPeriodRepository.findAllByPlaceIdOrderByDayOfWeekAscSequenceNoAsc(place.id)
        assertEquals(4.9, refreshedPlace.detailSnapshot!!.rating)
        assertEquals(99, refreshedPlace.detailSnapshot!!.userRatingCount)
        assertEquals(1, refreshedPeriods.size)
        assertEquals(3, refreshedPeriods.first().dayOfWeek)
        assertEquals(11 * 60, refreshedPeriods.first().openMinute)
    }

    @Test
    fun `wishlist API keeps response shape and like count sorting reacts to like and unlike`() {
        val owner = persistMember("sort-owner")
        val liker = persistMember("sort-liker")
        val trip = persistTrip("sort")
        persistTripMember(owner, trip, TripRole.OWNER)
        persistTripMember(liker, trip, TripRole.MEMBER)
        val ownerToken = accessToken(owner)
        val likerToken = accessToken(liker)

        `when`(placeSearchGateway.getPlaceDetails("sort-a", "ko")).thenReturn(details("sort-a", periodDay = 1))
        `when`(placeSearchGateway.getPlaceDetails("sort-b", "ko")).thenReturn(details("sort-b", periodDay = 2))
        `when`(placeSearchGateway.getPlaceDetails("sort-c", "ko")).thenReturn(details("sort-c", periodDay = 3))
        `when`(placeSearchGateway.getPlaceDetails("member-only", "ko")).thenReturn(details("member-only", periodDay = 4))

        val a = addTripWishlist(trip.id, ownerToken, "sort-a", "Sort A")
        val b = addTripWishlist(trip.id, ownerToken, "sort-b", "Sort B")
        val c = addTripWishlist(trip.id, ownerToken, "sort-c", "Sort C")
        addMemberWishlist(ownerToken, "member-only", "Member Only")

        mockMvc.perform(get("/api/v1/trips/${trip.id}/wishlists?wishlistSort=like_count_desc").auth(ownerToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].wishlistItemId", equalTo(c.wishlistItemId.toInt())))
            .andExpect(jsonPath("$.data.content[0].placeId", equalTo(c.placeId.toInt())))
            .andExpect(jsonPath("$.data.content[0].externalPlaceId", equalTo("sort-c")))
            .andExpect(jsonPath("$.data.content[0].adder.memberId", equalTo(owner.id.toInt())))
            .andExpect(jsonPath("$.data.content[0].likeCount", equalTo(0)))
            .andExpect(jsonPath("$.data.content[0].likedByMe", equalTo(false)))
            .andExpect(jsonPath("$.data.content[0].openingSummary.source", equalTo("REGULAR")))

        mockMvc.perform(get("/api/v1/members/me/wishlists").auth(ownerToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].memberWishlistItemId").exists())
            .andExpect(jsonPath("$.data.content[0].placeId").exists())
            .andExpect(jsonPath("$.data.content[0].externalPlaceId", equalTo("member-only")))
            .andExpect(jsonPath("$.data.content[0].openingSummary.source", equalTo("REGULAR")))

        like(trip.id, a.wishlistItemId, ownerToken)
        like(trip.id, a.wishlistItemId, likerToken)
        like(trip.id, b.wishlistItemId, likerToken)

        mockMvc.perform(get("/api/v1/trips/${trip.id}/wishlists?wishlistSort=like_count_desc").auth(ownerToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[*].externalPlaceId", contains("sort-a", "sort-b", "sort-c")))
            .andExpect(jsonPath("$.data.content[0].likeCount", equalTo(2)))
            .andExpect(jsonPath("$.data.content[0].likedByMe", equalTo(true)))

        mockMvc.perform(get("/api/v1/trips/${trip.id}/wishlists?wishlistSort=like_count_asc").auth(ownerToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[*].externalPlaceId", contains("sort-c", "sort-b", "sort-a")))
            .andExpect(jsonPath("$.data.content[0].likeCount", equalTo(0)))

        unlike(trip.id, a.wishlistItemId, ownerToken)

        mockMvc.perform(get("/api/v1/trips/${trip.id}/wishlists?wishlistSort=like_count_desc").auth(ownerToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[*].externalPlaceId", contains("sort-b", "sort-a", "sort-c")))
            .andExpect(jsonPath("$.data.content[0].likeCount", equalTo(1)))
            .andExpect(jsonPath("$.data.content[1].likeCount", equalTo(1)))
    }

    private fun persistMember(suffix: String): Member =
        memberRepository.save(
            Member(
                email = "$suffix@example.com",
                passwordHash = "pw",
                nickname = suffix,
            ),
        )

    private fun persistTrip(suffix: String): Trip =
        tripRepository.save(
            Trip(
                title = "Wishlist Flow $suffix",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 5, 3),
                country = Country.JAPAN,
            ),
        )

    private fun persistTripMember(
        member: Member,
        trip: Trip,
        role: TripRole,
    ): TripMember =
        tripMemberRepository.save(
            TripMember(
                member = member,
                trip = trip,
                role = role,
            ),
        )

    private fun addTripWishlist(
        tripId: Long,
        token: String,
        externalPlaceId: String,
        placeName: String,
    ): CreatedWishlistItem {
        val response = mockMvc.perform(
            post("/api/v1/trips/$tripId/wishlists")
                .auth(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishlistBody(externalPlaceId, placeName)),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val data = objectMapper.readTree(response).get("data")
        return CreatedWishlistItem(
            wishlistItemId = data.get("wishlistItemId").asLong(),
            placeId = data.get("placeId").asLong(),
        )
    }

    private fun addMemberWishlist(
        token: String,
        externalPlaceId: String,
        placeName: String,
    ) {
        mockMvc.perform(
            post("/api/v1/members/me/wishlists")
                .auth(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishlistBody(externalPlaceId, placeName)),
        )
            .andExpect(status().isCreated)
    }

    private fun like(
        tripId: Long,
        wishlistItemId: Long,
        token: String,
    ) {
        mockMvc.perform(post("/api/v1/trips/$tripId/wishlists/$wishlistItemId/likes").auth(token))
            .andExpect(status().isOk)
    }

    private fun unlike(
        tripId: Long,
        wishlistItemId: Long,
        token: String,
    ) {
        mockMvc.perform(delete("/api/v1/trips/$tripId/wishlists/$wishlistItemId/likes").auth(token))
            .andExpect(status().isOk)
    }

    private fun wishlistBody(
        externalPlaceId: String,
        placeName: String,
    ): String =
        """
        {
          "externalPlaceId": "$externalPlaceId",
          "placeName": "$placeName",
          "address": "Tokyo",
          "latitude": 35.6586000,
          "longitude": 139.7454000
        }
        """.trimIndent()

    private fun accessToken(member: Member): String =
        tokenProvider.createAccessToken(member.id, member.role)

    private fun MockHttpServletRequestBuilder.auth(token: String): MockHttpServletRequestBuilder =
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")

    private fun details(
        externalPlaceId: String,
        periodDay: Int,
        openMinute: Int = 10 * 60,
        rating: Double = 4.7,
        reviewCount: Int = 42,
    ): PlaceSearchGateway.DetailsPayload =
        PlaceSearchGateway.DetailsPayload(
            externalPlaceId = externalPlaceId,
            placeName = externalPlaceId,
            address = "Tokyo",
            latitude = 35.6586,
            longitude = 139.7454,
            primaryType = "tourist_attraction",
            types = listOf("tourist_attraction", "point_of_interest"),
            businessStatus = "OPERATIONAL",
            utcOffsetMinutes = 540,
            rating = rating,
            userRatingCount = reviewCount,
            regularOpeningHoursJson = """{"periods":[]}""",
            currentOpeningHoursJson = """{"openNow":true}""",
            primaryPhotoName = "places/$externalPlaceId/photos/primary",
            editorialSummary = "summary-$externalPlaceId",
            regularOpeningPeriods = listOf(
                PlaceSearchGateway.RegularOpeningPeriodInput(
                    dayOfWeek = periodDay,
                    openMinute = openMinute,
                    closeMinute = openMinute + 8 * 60,
                    isOvernight = false,
                    sequenceNo = 1,
                ),
            ),
        )

    private data class CreatedWishlistItem(
        val wishlistItemId: Long,
        val placeId: Long,
    )
}
