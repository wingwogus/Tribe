package com.tribe.api.itinerary.wishlist

import com.tribe.application.itinerary.place.OpeningSummaryAssembler
import com.tribe.application.itinerary.place.OpeningSummarySource
import com.tribe.domain.config.QueryDslConfig
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.itinerary.wishlist.AccountWishlistSort
import com.tribe.domain.itinerary.wishlist.MemberWishlistItem
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.itinerary.wishlist.TripWishlistSort
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.itinerary.wishlist.WishlistItemLike
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripRole
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@DataJpaTest(properties = ["spring.jpa.properties.hibernate.generate_statistics=true"])
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class WishlistRepositoryIntegrationTest(
    @Autowired private val entityManager: TestEntityManager,
    @Autowired private val entityManagerFactory: EntityManagerFactory,
    @Autowired private val placeRepository: PlaceRepository,
    @Autowired private val memberWishlistItemRepository: MemberWishlistItemRepository,
    @Autowired private val wishlistItemRepository: WishlistItemRepository,
) {
    @Test
    fun `member wishlist sorts by place detail fields using database query`() {
        val member = persistMember("account")
        persistMemberWishlistItem(member, persistPlace("high-many", rating = 4.8, reviewCount = 50))
        persistMemberWishlistItem(member, persistPlace("high-few", rating = 4.8, reviewCount = 5))
        persistMemberWishlistItem(member, persistPlace("mid-most", rating = 4.7, reviewCount = 500))
        persistMemberWishlistItem(member, persistPlace("unrated", rating = null, reviewCount = null))
        flushAndClear()

        assertEquals(
            listOf("high-few", "high-many", "mid-most", "unrated"),
            memberWishlistNames(member.id, AccountWishlistSort.RATING_DESC),
        )
        assertEquals(
            listOf("mid-most", "high-many", "high-few", "unrated"),
            memberWishlistNames(member.id, AccountWishlistSort.REVIEW_COUNT_DESC),
        )
        assertEquals(
            listOf("high-many", "high-few", "mid-most", "unrated"),
            memberWishlistNames(member.id, AccountWishlistSort.REVIEW_GOOD_DESC),
        )
    }

    @Test
    fun `trip wishlist sorts by place detail and like counts using database query`() {
        val trip = persistTrip()
        val adder = persistTripMember(trip, "adder")
        val likerA = persistTripMember(trip, "liker-a")
        val likerB = persistTripMember(trip, "liker-b")
        val likerC = persistTripMember(trip, "liker-c")
        val highMany = persistWishlistItem(trip, adder, persistPlace("high-many", rating = 4.8, reviewCount = 50))
        val highFew = persistWishlistItem(trip, adder, persistPlace("high-few", rating = 4.8, reviewCount = 5))
        val midMost = persistWishlistItem(trip, adder, persistPlace("mid-most", rating = 4.7, reviewCount = 500))
        persistWishlistItem(trip, adder, persistPlace("unrated", rating = null, reviewCount = null))
        persistLike(highMany, likerA)
        persistLike(highMany, likerB)
        persistLike(highMany, likerC)
        persistLike(highFew, likerA)
        persistLike(highFew, likerB)
        persistLike(midMost, likerA)
        flushAndClear()

        assertEquals(
            listOf("high-few", "high-many", "mid-most", "unrated"),
            tripWishlistNames(trip.id, TripWishlistSort.RATING_DESC),
        )
        assertEquals(
            listOf("mid-most", "high-many", "high-few", "unrated"),
            tripWishlistNames(trip.id, TripWishlistSort.REVIEW_COUNT_DESC),
        )
        assertEquals(
            listOf("high-many", "high-few", "mid-most", "unrated"),
            tripWishlistNames(trip.id, TripWishlistSort.REVIEW_GOOD_DESC),
        )
        assertEquals(
            listOf("high-many", "high-few", "mid-most", "unrated"),
            tripWishlistNames(trip.id, TripWishlistSort.LIKE_COUNT_DESC),
        )
        assertEquals(
            listOf("unrated", "mid-most", "high-few", "high-many"),
            tripWishlistNames(trip.id, TripWishlistSort.LIKE_COUNT_ASC),
        )
    }

    @Test
    fun `trip wishlist opening summary access batches regular opening periods`() {
        val trip = persistTrip()
        val adder = persistTripMember(trip, "adder")
        repeat(12) { index ->
            persistWishlistItem(trip, adder, persistPlaceWithRegularHours("regular-$index"))
        }
        flushAndClear()
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.clear()

        val page = wishlistItemRepository.findPageByTrip(trip.id, null, null, PageRequest.of(0, 10))
        val summaries = page.content.map {
            OpeningSummaryAssembler(
                clock = Clock.fixed(Instant.parse("2026-05-17T03:30:00Z"), ZoneOffset.UTC),
            ).toOpeningSummary(it.place)
        }

        assertEquals(10, summaries.size)
        assertTrue(summaries.all { it?.source == OpeningSummarySource.REGULAR })
        assertTrue(summaries.all { it?.openNow == true })
        assertTrue(summaries.all { it?.nextCloseTime == "2026-05-17T18:00+09:00" })
        assertTrue(
            statistics.prepareStatementCount <= 4,
            "expected bounded wishlist queries with one batched opening-period load but got ${statistics.prepareStatementCount}",
        )
    }

    @Test
    fun `member wishlist opening summary access batches regular opening periods`() {
        val member = persistMember("account-regular")
        repeat(12) { index ->
            persistMemberWishlistItem(member, persistPlaceWithRegularHours("member-regular-$index"))
        }
        flushAndClear()
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.clear()

        val page = memberWishlistItemRepository.findPageByMember(member.id, null, null, PageRequest.of(0, 10))
        val summaries = page.content.map {
            OpeningSummaryAssembler(
                clock = Clock.fixed(Instant.parse("2026-05-17T03:30:00Z"), ZoneOffset.UTC),
            ).toOpeningSummary(it.place)
        }

        assertEquals(10, summaries.size)
        assertTrue(summaries.all { it?.source == OpeningSummarySource.REGULAR })
        assertTrue(summaries.all { it?.openNow == true })
        assertTrue(summaries.all { it?.nextCloseTime == "2026-05-17T18:00+09:00" })
        assertTrue(
            statistics.prepareStatementCount <= 4,
            "expected bounded member wishlist queries with one batched opening-period load but got ${statistics.prepareStatementCount}",
        )
    }

    @Test
    fun `active wishlist place refresh query returns distinct places still in wishlists`() {
        val member = persistMember("active-account")
        val trip = persistTrip()
        val adder = persistTripMember(trip, "active-adder")
        val shared = persistPlace("shared-active", rating = null, reviewCount = null)
        val tripOnly = persistPlace("trip-active", rating = null, reviewCount = null)
        val memberOnly = persistPlace("member-active", rating = null, reviewCount = null)
        persistPlace("inactive", rating = null, reviewCount = null)
        persistWishlistItem(trip, adder, shared)
        persistWishlistItem(trip, adder, tripOnly)
        persistMemberWishlistItem(member, shared)
        persistMemberWishlistItem(member, memberOnly)
        flushAndClear()

        val detailsCutoff = LocalDateTime.now().minusDays(30)
        val regularHoursCutoff = LocalDateTime.now().minusDays(30)
        val currentHoursCutoff = LocalDateTime.now().minusDays(7)
        val activePlaceIds = placeRepository.findActiveWishlistedPlacesForRefresh(
            detailsCutoff,
            regularHoursCutoff,
            currentHoursCutoff,
            PageRequest.of(0, 10),
        )
            .map { it.externalPlaceId }
            .toSet()
        val limitedPlaces = placeRepository.findActiveWishlistedPlacesForRefresh(
            detailsCutoff,
            regularHoursCutoff,
            currentHoursCutoff,
            PageRequest.of(0, 2),
        )

        assertEquals(
            setOf("place-shared-active", "place-trip-active", "place-member-active"),
            activePlaceIds,
        )
        assertEquals(2, limitedPlaces.size)
        assertTrue(limitedPlaces.all { it.externalPlaceId in activePlaceIds })
    }

    private fun memberWishlistNames(
        memberId: Long,
        sort: AccountWishlistSort,
    ): List<String> =
        memberWishlistItemRepository.findPageByMember(memberId, null, sort, PageRequest.of(0, 10))
            .content
            .map { it.place.name }

    private fun tripWishlistNames(
        tripId: Long,
        sort: TripWishlistSort,
    ): List<String> =
        wishlistItemRepository.findPageByTrip(tripId, null, sort, PageRequest.of(0, 10))
            .content
            .map { it.place.name }

    private fun persistMember(suffix: String): Member =
        entityManager.persist(
            Member(
                email = "$suffix@example.com",
                passwordHash = "pw",
                nickname = "member-$suffix",
            ),
        )

    private fun persistTrip(): Trip =
        entityManager.persist(
            Trip(
                title = "Wishlist Sort Trip",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 5, 3),
                country = Country.JAPAN,
            ),
        )

    private fun persistTripMember(
        trip: Trip,
        suffix: String,
    ): TripMember =
        entityManager.persist(
            TripMember(
                member = persistMember(suffix),
                trip = trip,
                role = TripRole.MEMBER,
            ),
        )

    private fun persistPlace(
        name: String,
        rating: Double?,
        reviewCount: Int?,
    ): Place {
        val place = entityManager.persist(
            Place(
                externalPlaceId = "place-$name",
                name = name,
                address = "Tokyo",
                latitude = BigDecimal("35.0000000"),
                longitude = BigDecimal("139.0000000"),
            ),
        )
        val snapshot = entityManager.persist(
            PlaceDetailSnapshot(
                place = place,
                rating = rating,
                userRatingCount = reviewCount,
            ),
        )
        place.detailSnapshot = snapshot
        return place
    }

    private fun persistPlaceWithRegularHours(name: String): Place {
        val place = entityManager.persist(
            Place(
                externalPlaceId = "place-$name",
                name = name,
                address = "Tokyo",
                latitude = BigDecimal("35.0000000"),
                longitude = BigDecimal("139.0000000"),
                utcOffsetMinutes = 540,
            ),
        )
        val snapshot = entityManager.persist(
            PlaceDetailSnapshot(
                place = place,
                openingHoursSyncedAt = LocalDateTime.of(2026, 5, 17, 0, 0),
            ),
        )
        place.detailSnapshot = snapshot
        val period = entityManager.persist(
            PlaceRegularOpeningPeriod(
                place = place,
                dayOfWeek = 0,
                openMinute = 9 * 60,
                closeMinute = 18 * 60,
                isOvernight = false,
                sequenceNo = 1,
            ),
        )
        place.regularOpeningPeriods.add(period)
        return place
    }

    private fun persistMemberWishlistItem(
        member: Member,
        place: Place,
    ): MemberWishlistItem =
        entityManager.persist(MemberWishlistItem(member, place))

    private fun persistWishlistItem(
        trip: Trip,
        adder: TripMember,
        place: Place,
    ): WishlistItem =
        entityManager.persist(WishlistItem(trip, place, adder))

    private fun persistLike(
        wishlistItem: WishlistItem,
        tripMember: TripMember,
    ): WishlistItemLike =
        entityManager.persist(WishlistItemLike(wishlistItem, tripMember))

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
