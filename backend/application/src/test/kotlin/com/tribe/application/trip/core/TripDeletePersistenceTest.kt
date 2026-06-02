package com.tribe.application.trip.core

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.config.QueryDslConfig
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.LocalDate

@DataJpaTest
@ContextConfiguration(classes = [TripDeletePersistenceTest.TestApplication::class])
@Import(QueryDslConfig::class)
class TripDeletePersistenceTest(
    @Autowired private val entityManager: TestEntityManager,
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val placeRepository: PlaceRepository,
    @Autowired private val tripRepository: TripRepository,
    @Autowired private val wishlistItemRepository: WishlistItemRepository,
) {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = ["com.tribe.domain"])
    @EnableJpaRepositories(basePackages = ["com.tribe.domain"])
    class TestApplication

    @Test
    fun `deleting trip removes trip wishlist items before trip members`() {
        val member = memberRepository.save(
            Member(email = "owner@example.com", passwordHash = "hashed", nickname = "owner"),
        )
        val trip = tripRepository.save(
            Trip(
                title = "Tokyo",
                startDate = LocalDate.of(2026, 4, 12),
                endDate = LocalDate.of(2026, 4, 14),
                country = Country.JAPAN,
            ).apply {
                addMember(member, TripRole.OWNER)
            },
        )
        val place = placeRepository.save(
            Place(
                externalPlaceId = "tokyo-tower",
                name = "Tokyo Tower",
                address = "Tokyo",
                latitude = BigDecimal("35.6586000"),
                longitude = BigDecimal("139.7454000"),
            ),
        )
        val wishlistItem = WishlistItem(
            trip = trip,
            place = place,
            adder = trip.members.single(),
        )
        trip.wishlistItems.add(wishlistItem)
        wishlistItemRepository.saveAndFlush(wishlistItem)
        entityManager.clear()

        wishlistItemRepository.deleteByTripId(trip.id)
        val persistedTrip = tripRepository.findById(trip.id).orElseThrow()
        tripRepository.delete(persistedTrip)
        tripRepository.flush()

        assertFalse(tripRepository.existsById(trip.id))
        assertFalse(wishlistItemRepository.existsById(wishlistItem.id))
    }
}
