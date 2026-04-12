package com.tribe.domain.trip.review

import org.springframework.data.jpa.repository.JpaRepository

interface RecommendedPlaceRepository : JpaRepository<RecommendedPlace, Long>
