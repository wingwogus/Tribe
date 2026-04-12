package com.tribe.domain.itinerary.category

import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.trip.core.Trip
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
class Category(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    val trip: Trip,
    @Column(name = "category_day", nullable = false)
    var day: Int,
    @Column(nullable = false)
    var name: String,
    @Column(name = "category_order", nullable = false)
    var order: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    val id: Long = 0L

    @Column(columnDefinition = "TEXT")
    var memo: String? = null

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    val itineraryItems: MutableList<ItineraryItem> = mutableListOf()

    fun updateOrder(newOrder: Int) {
        order = newOrder
    }
}
