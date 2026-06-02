package com.tribe.domain.trip.review

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * 여행 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface TripReviewRepository : JpaRepository<TripReview, Long> {
    fun findTripReviewsByTripId(tripId: Long, pageable: Pageable): Page<TripReview>

    @Query(
        """
        select t from TripReview t
        left join fetch t.recommendedPlaces rp
        left join fetch rp.place p
        where t.id = :tripReviewId
        """
    )
    fun findTripReviewWithRecommendedPlacesById(tripReviewId: Long): TripReview?
}
