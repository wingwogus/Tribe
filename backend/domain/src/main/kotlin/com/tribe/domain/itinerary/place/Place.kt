package com.tribe.domain.itinerary.place

import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.trip.review.RecommendedPlace
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import java.math.BigDecimal

@Entity
class Place(
    @Column(nullable = false, unique = true)
    val externalPlaceId: String,
    @Column(nullable = false)
    val name: String,
    val address: String? = null,
    @Column(precision = 10, scale = 7)
    val latitude: BigDecimal,
    @Column(precision = 10, scale = 7)
    val longitude: BigDecimal,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    val id: Long = 0L

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY)
    val itineraryItems: MutableList<ItineraryItem> = mutableListOf()

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val wishlistItems: MutableList<WishlistItem> = mutableListOf()

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val recommendedPlaces: MutableList<RecommendedPlace> = mutableListOf()
}
