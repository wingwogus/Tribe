package com.tribe.domain.itinerary.place

import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRepository : JpaRepository<Place, Long> {
    fun findByExternalPlaceId(externalPlaceId: String): Place?
}
