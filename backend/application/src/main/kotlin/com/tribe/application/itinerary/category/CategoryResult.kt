package com.tribe.application.itinerary.category

import com.tribe.domain.itinerary.category.Category
import java.time.LocalDateTime

object CategoryResult {
    data class CategoryView(
        val categoryId: Long,
        val name: String,
        val day: Int,
        val order: Int,
        val tripId: Long,
        val memo: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(category: Category) = CategoryView(
                categoryId = category.id,
                name = category.name,
                day = category.day,
                order = category.order,
                tripId = category.trip.id,
                memo = category.memo,
                createdAt = category.createdAt,
                updatedAt = category.updatedAt,
            )
        }
    }
}
