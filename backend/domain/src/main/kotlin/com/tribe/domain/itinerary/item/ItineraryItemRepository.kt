package com.tribe.domain.itinerary.item

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItineraryItemRepository : JpaRepository<ItineraryItem, Long> {
    fun findByTripIdAndVisitDayOrderByOrderAsc(tripId: Long, visitDay: Int): List<ItineraryItem>
    fun countByTripIdAndVisitDay(tripId: Long, visitDay: Int): Int

    @Query("select i from ItineraryItem i where i.id in :itemIds and i.trip.id = :tripId")
    fun findByIdInAndTripId(@Param("itemIds") itemIds: List<Long>, @Param("tripId") tripId: Long): List<ItineraryItem>

    @Query(
        """
        select i from ItineraryItem i
        where i.trip.id = :tripId
        order by i.visitDay asc, i.order asc
        """
    )
    fun findByTripIdOrderByVisitDayAndOrder(@Param("tripId") tripId: Long): List<ItineraryItem>
}
