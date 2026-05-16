package com.tribe.domain.itinerary.wishlist

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.member.Member
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
    name = "member_wishlist_items",
    indexes = [
        Index(name = "idx_member_wishlist_items_member_id", columnList = "member_id"),
        Index(name = "idx_member_wishlist_items_place_id", columnList = "place_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_member_wishlist_items_member_place", columnNames = ["member_id", "place_id"]),
    ],
)
class MemberWishlistItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    val place: Place,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_wishlist_item_id")
    val id: Long = 0L
}
