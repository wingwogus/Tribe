package com.tribe.application.trip.core

import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

class TripResultTest {
    @Test
    fun `simple and detail trip preserve distinct country semantics`() {
        val trip = Trip(
            title = "Japan",
            startDate = LocalDate.of(2026, 4, 12),
            endDate = LocalDate.of(2026, 4, 13),
            country = Country.JAPAN,
        )
        ReflectionTestUtils.setField(trip, "id", 5L)
        val member = Member(
            id = 1L,
            email = "user@test.com",
            passwordHash = "pw",
            nickname = "owner",
            avatar = "https://cdn.example.com/owner.png",
        )
        trip.members.add(TripMember(member = member, trip = trip, role = TripRole.OWNER))

        val simple = TripResult.SimpleTrip.from(trip)
        val detail = TripResult.TripDetail.from(trip)

        assertEquals("일본", simple.country)
        assertEquals("JP", detail.country)
        assertEquals("https://cdn.example.com/owner.png", simple.members.single().avatar)
        assertEquals("https://cdn.example.com/owner.png", detail.members.single().avatar)
        assertNotEquals(simple.country, detail.country)
    }
}
