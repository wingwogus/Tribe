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
import jakarta.persistence.OneToOne
import org.hibernate.annotations.BatchSize
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 내부 canonical 장소.
 *
 * Google externalPlaceId를 unique identity로 삼아 일정/위시/리뷰가 공유.
 */
@Entity
class Place(
    // Google place id, 내부 중복 저장 방지 기준.
    @Column(nullable = false, unique = true)
    val externalPlaceId: String,
    @Column(nullable = false)
    val name: String,
    val address: String? = null,
    @Column(precision = 10, scale = 7)
    val latitude: BigDecimal,
    @Column(precision = 10, scale = 7)
    val longitude: BigDecimal,
    @Column(name = "google_primary_type")
    var googlePrimaryType: String? = null,
    @Column(name = "google_types_json", columnDefinition = "TEXT")
    var googleTypesJson: String? = null,
    // 목록/상세에서 바로 필요한 Google business 상태 요약.
    @Column(name = "business_status")
    var businessStatus: String? = null,
    @Column(name = "utc_offset_minutes")
    var utcOffsetMinutes: Int? = null,
    @Column(name = "type_summary_synced_at")
    var typeSummarySyncedAt: LocalDateTime? = null,
    // null이면 상세 snapshot/영업시간 보강 대상.
    @Column(name = "details_synced_at")
    var detailsSyncedAt: LocalDateTime? = null,
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

    @OneToOne(mappedBy = "place", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var detailSnapshot: PlaceDetailSnapshot? = null

    // Google regularOpeningHours를 계산 가능한 period 목록으로 분리 저장.
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val regularOpeningPeriods: MutableList<PlaceRegularOpeningPeriod> = mutableListOf()
}
