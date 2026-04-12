package com.tribe.domain.itinerary.category

import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findAllByTripIdOrderByDayAscOrderAsc(tripId: Long): List<Category>
    fun findAllByTripIdAndDayOrderByOrderAsc(tripId: Long, day: Int): List<Category>
    fun findAllByTripIdAndIdIn(tripId: Long, ids: List<Long>): List<Category>
}
