package com.tribe.domain.itinerary.wishlist

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WishlistItemRepository : JpaRepository<WishlistItem, Long> {
    @Query(
        value = """
            select wi from WishlistItem wi
            join fetch wi.place p
            join fetch wi.adder a
            left join fetch a.member m
            where wi.trip.id = :tripId
        """,
        countQuery = "select count(wi) from WishlistItem wi where wi.trip.id = :tripId",
    )
    fun findAllByTrip_Id(@Param("tripId") tripId: Long, pageable: Pageable): Page<WishlistItem>

    @Query(
        value = """
            select wi from WishlistItem wi
            join fetch wi.place p
            join fetch wi.adder a
            left join fetch a.member m
            where wi.trip.id = :tripId and lower(p.name) like lower(concat('%', :query, '%'))
        """,
        countQuery = """
            select count(wi) from WishlistItem wi
            join wi.place p
            where wi.trip.id = :tripId and lower(p.name) like lower(concat('%', :query, '%'))
        """,
    )
    fun findAllByTrip_IdAndPlace_NameContainingIgnoreCase(
        @Param("tripId") tripId: Long,
        @Param("query") query: String,
        pageable: Pageable,
    ): Page<WishlistItem>

    fun existsByTrip_IdAndPlace_ExternalPlaceId(tripId: Long, externalPlaceId: String): Boolean

    @Query("select w.id from WishlistItem w where w.trip.id = :tripId and w.id in :ids")
    fun findIdsByTripIdAndIdIn(@Param("tripId") tripId: Long, @Param("ids") ids: List<Long>): List<Long>

    @Modifying
    @Query("delete from WishlistItem w where w.adder.id = :adderId")
    fun deleteByAdderId(adderId: Long)
}
