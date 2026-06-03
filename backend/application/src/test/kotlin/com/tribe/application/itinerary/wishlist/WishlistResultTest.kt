package com.tribe.application.itinerary.wishlist

import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate

class WishlistResultTest {
    @Test
    fun `item from applies saved place-derived fragments`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val member = Member(id = 1L, email = "user@test.com", passwordHash = "pw", nickname = "adder")
        val tripMember = TripMember(member = member, trip = trip, role = TripRole.MEMBER)
        ReflectionTestUtils.setField(tripMember, "id", 30L)
        val place = placeFixture()
        val wishlistItem = WishlistItem(trip = trip, place = place, adder = tripMember)
        ReflectionTestUtils.setField(wishlistItem, "id", 50L)

        val result = WishlistResult.Item.from(wishlistItem)

        assertEquals(50L, result.wishlistItemId)
        assertEquals("cafe", result.placeTypeSummary?.primaryType)
        assertEquals("cafe", result.placeTypeSummary?.displayPrimaryLabel)
        assertEquals(NormalizedPlaceCategoryKey.CAFE, result.normalizedCategoryKey)
        assertEquals("OPERATIONAL", result.placeDetailSummary?.businessStatus)
        assertEquals(4.3, result.placeDetailSummary?.rating)
        assertEquals(41, result.placeDetailSummary?.userRatingCount)
        assertEquals("조용한 카페", result.placeDetailSummary?.editorialSummary)
        assertEquals(30L, result.adder.tripMemberId)
        assertEquals(1L, result.adder.memberId)
        assertEquals("adder", result.adder.nickname)
    }

    private fun placeFixture(): Place {
        val place = Place(
            externalPlaceId = "place-1",
            name = "Cafe",
            address = "Osaka",
            latitude = BigDecimal("34.6937"),
            longitude = BigDecimal("135.5023"),
        )
        ReflectionTestUtils.setField(place, "id", 11L)
        place.googlePrimaryType = "cafe"
        place.googleTypesJson = "[\"cafe\",\"food\"]"
        place.businessStatus = "OPERATIONAL"
        place.detailSnapshot = PlaceDetailSnapshot(
            place = place,
            rating = 4.3,
            userRatingCount = 41,
            editorialSummary = "조용한 카페",
        )
        return place
    }
}
