package com.tribe.domain.itinerary.item

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItineraryItemRepository : JpaRepository<ItineraryItem, Long> {
    fun findByCategoryIdOrderByOrderAsc(categoryId: Long): List<ItineraryItem>
    fun countByCategoryId(categoryId: Long): Int

    @Query("select i from ItineraryItem i join i.category c where i.id in :itemIds and c.trip.id = :tripId")
    fun findByIdInAndTripId(@Param("itemIds") itemIds: List<Long>, @Param("tripId") tripId: Long): List<ItineraryItem>

    @Query(
        """
        select i from ItineraryItem i
        join i.category c
        where c.trip.id = :tripId
        order by c.day asc, c.order asc, i.order asc
        """
    )
    fun findByTripIdOrderByCategoryAndOrder(@Param("tripId") tripId: Long): List<ItineraryItem>
}
