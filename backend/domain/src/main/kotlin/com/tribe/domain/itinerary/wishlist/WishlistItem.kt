package com.tribe.domain.itinerary.wishlist

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Column

@Entity
class WishlistItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    val trip: Trip,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    val place: Place,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adder_id", nullable = false)
    val adder: TripMember,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_item_id")
    val id: Long = 0L
}
