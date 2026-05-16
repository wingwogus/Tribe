package com.tribe.domain.itinerary.wishlist

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberWishlistItemRepository : JpaRepository<MemberWishlistItem, Long>, MemberWishlistItemRepositoryCustom {
    @Query(
        value = """
            select mwi from MemberWishlistItem mwi
            join fetch mwi.place p
            where mwi.member.id = :memberId
        """,
        countQuery = "select count(mwi) from MemberWishlistItem mwi where mwi.member.id = :memberId",
    )
    fun findAllByMember_Id(@Param("memberId") memberId: Long, pageable: Pageable): Page<MemberWishlistItem>

    @Query(
        value = """
            select mwi from MemberWishlistItem mwi
            join fetch mwi.place p
            where mwi.member.id = :memberId and lower(p.name) like lower(concat('%', :query, '%'))
        """,
        countQuery = """
            select count(mwi) from MemberWishlistItem mwi
            join mwi.place p
            where mwi.member.id = :memberId and lower(p.name) like lower(concat('%', :query, '%'))
        """,
    )
    fun findAllByMember_IdAndPlace_NameContainingIgnoreCase(
        @Param("memberId") memberId: Long,
        @Param("query") query: String,
        pageable: Pageable,
    ): Page<MemberWishlistItem>

    @Query(
        """
            select mwi from MemberWishlistItem mwi
            join fetch mwi.place p
            where mwi.id = :id and mwi.member.id = :memberId
        """,
    )
    fun findByIdAndMemberIdWithPlace(
        @Param("id") id: Long,
        @Param("memberId") memberId: Long,
    ): MemberWishlistItem?

    fun existsByMember_IdAndPlace_ExternalPlaceId(memberId: Long, externalPlaceId: String): Boolean

    @Query("select mwi.id from MemberWishlistItem mwi where mwi.member.id = :memberId and mwi.id in :ids")
    fun findIdsByMemberIdAndIdIn(@Param("memberId") memberId: Long, @Param("ids") ids: List<Long>): List<Long>
}
