package com.tribe.domain.itinerary.place

import org.springframework.data.jpa.repository.JpaRepository

/**
 * 장소 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface PlaceRegularOpeningPeriodRepository : JpaRepository<PlaceRegularOpeningPeriod, Long> {
    fun deleteAllByPlaceId(placeId: Long)
    fun findAllByPlaceIdOrderByDayOfWeekAscSequenceNoAsc(placeId: Long): List<PlaceRegularOpeningPeriod>
}
