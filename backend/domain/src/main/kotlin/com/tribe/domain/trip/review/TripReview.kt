package com.tribe.domain.trip.review

import com.tribe.domain.trip.core.Trip
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
class TripReview(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    val trip: Trip,
    val concept: String?,
    @Lob
    var content: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_review_id")
    val id: Long = 0L

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tripReview", cascade = [CascadeType.ALL], orphanRemoval = true)
    val recommendedPlaces: MutableList<RecommendedPlace> = mutableListOf()
}
