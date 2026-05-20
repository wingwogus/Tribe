package com.tribe.domain.itinerary.place

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PlaceRepository : JpaRepository<Place, Long> {
    fun findByExternalPlaceId(externalPlaceId: String): Place?
    fun findByExternalPlaceIdIn(externalPlaceIds: Collection<String>): List<Place>

    @Query(
        """
            select distinct p from Place p
            left join fetch p.detailSnapshot ds
            where (
                exists (
                    select 1 from WishlistItem wi
                    where wi.place = p
                ) or exists (
                    select 1 from MemberWishlistItem mwi
                    where mwi.place = p
                )
            )
            and (
                p.detailsSyncedAt is null
                or p.detailsSyncedAt < :detailsCutoff
                or ds.placeId is null
                or ds.openingHoursSyncedAt is null
                or ds.openingHoursSyncedAt < :regularHoursCutoff
                or ds.currentOpeningHoursSyncedAt is null
                or ds.currentOpeningHoursSyncedAt < :currentHoursCutoff
            )
            order by coalesce(ds.currentOpeningHoursSyncedAt, ds.openingHoursSyncedAt, ds.detailsSyncedAt, p.detailsSyncedAt), p.id
        """,
    )
    fun findActiveWishlistedPlacesForRefresh(
        @Param("detailsCutoff") detailsCutoff: LocalDateTime,
        @Param("regularHoursCutoff") regularHoursCutoff: LocalDateTime,
        @Param("currentHoursCutoff") currentHoursCutoff: LocalDateTime,
        pageable: Pageable,
    ): List<Place>
}
