package com.tribe.domain.itinerary.place

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import java.time.LocalDateTime

@Entity
class PlaceDetailSnapshot(
    @Id
    @Column(name = "place_id")
    val placeId: Long = 0L,
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    val place: Place,
    @Column(name = "formatted_phone_number")
    var formattedPhoneNumber: String? = null,
    @Column(name = "international_phone_number")
    var internationalPhoneNumber: String? = null,
    @Column(name = "website_uri")
    var websiteUri: String? = null,
    @Column(name = "google_maps_uri")
    var googleMapsUri: String? = null,
    var rating: Double? = null,
    @Column(name = "user_rating_count")
    var userRatingCount: Int? = null,
    @Column(name = "price_level")
    var priceLevel: Int? = null,
    @Column(name = "regular_opening_hours_json", columnDefinition = "TEXT")
    var regularOpeningHoursJson: String? = null,
    @Column(name = "current_opening_hours_json", columnDefinition = "TEXT")
    var currentOpeningHoursJson: String? = null,
    @Column(name = "opening_hours_synced_at")
    var openingHoursSyncedAt: LocalDateTime? = null,
    @Column(name = "current_opening_hours_synced_at")
    var currentOpeningHoursSyncedAt: LocalDateTime? = null,
    @Column(name = "primary_photo_name", columnDefinition = "TEXT")
    var primaryPhotoName: String? = null,
    @Column(name = "editorial_summary", columnDefinition = "TEXT")
    var editorialSummary: String? = null,
    @Column(name = "details_synced_at")
    var detailsSyncedAt: LocalDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
