package com.tribe.domain.itinerary.place

import org.springframework.data.jpa.repository.JpaRepository

/**
 * 장소 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface PlaceRepository : JpaRepository<Place, Long> {
    fun findByExternalPlaceId(externalPlaceId: String): Place?
    fun findByExternalPlaceIdIn(externalPlaceIds: Collection<String>): List<Place>
}
