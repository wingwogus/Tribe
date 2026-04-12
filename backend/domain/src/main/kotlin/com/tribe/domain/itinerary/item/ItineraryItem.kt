package com.tribe.domain.itinerary.item

import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.place.Place
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class ItineraryItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    var place: Place?,
    var title: String?,
    var time: LocalDateTime?,
    @Column(name = "item_order", nullable = false)
    var order: Int,
    @Lob
    var memo: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    val id: Long = 0L
}
