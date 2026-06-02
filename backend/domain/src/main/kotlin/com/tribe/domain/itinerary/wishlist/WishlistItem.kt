package com.tribe.domain.itinerary.wishlist

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Column
import jakarta.persistence.Table

/**
 * 위시리스트 도메인 상태 모델.
 *
 * 영속성 identity와 업무 규칙의 기준점.
 */
@Entity
@Table(
    name = "wishlist_item",
    indexes = [
        Index(name = "idx_wishlist_item_trip_id", columnList = "trip_id"),
        Index(name = "idx_wishlist_item_place_id", columnList = "place_id"),
        Index(name = "idx_wishlist_item_adder_id", columnList = "adder_id"),
    ],
)
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
