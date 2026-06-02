package com.tribe.domain.trip.review

import org.springframework.data.jpa.repository.JpaRepository

/**
 * 여행 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface RecommendedPlaceRepository : JpaRepository<RecommendedPlace, Long>
