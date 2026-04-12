package com.tribe.domain.trip.review

import com.tribe.domain.itinerary.place.Place
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Column

@Entity
class RecommendedPlace(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    val place: Place,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_review_id")
    val tripReview: TripReview,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommended_place_id")
    val id: Long = 0L

    companion object {
        fun from(place: Place, tripReview: TripReview): RecommendedPlace {
            val recommendedPlace = RecommendedPlace(place, tripReview)
            tripReview.recommendedPlaces.add(recommendedPlace)
            return recommendedPlace
        }
    }
}
