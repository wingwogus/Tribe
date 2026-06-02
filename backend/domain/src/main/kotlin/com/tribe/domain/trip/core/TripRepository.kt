package com.tribe.domain.trip.core

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 여행 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface TripRepository : JpaRepository<Trip, Long> {
    @Query(
        """
        select t
        from Trip t
        join t.members tm
        where tm.member.id = :memberId
          and tm.role not in ('KICKED', 'EXITED')
        """
    )
    fun findTripsByMemberId(@Param("memberId") memberId: Long, pageable: Pageable): Page<Trip>

    @Query(
        """
        select distinct t
        from Trip t
        join fetch t.members m
        left join fetch m.member
        where t.id = :tripId
        """
    )
    fun findTripWithMembersById(@Param("tripId") tripId: Long): Trip?

    @Query(
        """
        select distinct t
        from Trip t
        left join fetch t.itineraryItems i
        left join fetch i.place p
        where t.id = :tripId
        """
    )
    fun findTripWithFullItineraryById(@Param("tripId") tripId: Long): Trip?
}
