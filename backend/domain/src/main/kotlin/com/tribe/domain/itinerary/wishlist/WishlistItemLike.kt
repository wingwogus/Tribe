package com.tribe.domain.itinerary.wishlist

import com.tribe.domain.trip.member.TripMember
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "wishlist_item_likes",
    indexes = [
        Index(name = "idx_wishlist_item_likes_wishlist_item_id", columnList = "wishlist_item_id"),
        Index(name = "idx_wishlist_item_likes_trip_member_id", columnList = "trip_member_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_wishlist_item_likes_item_member",
            columnNames = ["wishlist_item_id", "trip_member_id"],
        ),
    ],
)
class WishlistItemLike(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_item_id", nullable = false)
    val wishlistItem: WishlistItem,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_member_id", nullable = false)
    val tripMember: TripMember,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_item_like_id")
    val id: Long = 0L
}
