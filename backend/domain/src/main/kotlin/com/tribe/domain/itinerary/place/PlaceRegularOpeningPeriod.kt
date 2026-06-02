package com.tribe.domain.itinerary.place

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

/**
 * 장소 정규 영업시간 period.
 *
 * Google 요일/분 단위 period를 일정 시간 판정용으로 정규화.
 */
@Entity
class PlaceRegularOpeningPeriod(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    val place: Place,
    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: Int,
    @Column(name = "open_minute", nullable = false)
    var openMinute: Int,
    @Column(name = "close_minute", nullable = false)
    var closeMinute: Int,
    // 자정 이후 closeMinute까지 이어지는 전날 영업 period 여부.
    @Column(name = "is_overnight", nullable = false)
    var isOvernight: Boolean = false,
    @Column(name = "sequence_no", nullable = false)
    var sequenceNo: Int = 1,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_regular_opening_period_id")
    val id: Long = 0L
}
