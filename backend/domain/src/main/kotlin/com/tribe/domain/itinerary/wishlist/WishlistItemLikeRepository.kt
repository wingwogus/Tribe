package com.tribe.domain.itinerary.wishlist

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WishlistItemLikeRepository : JpaRepository<WishlistItemLike, Long> {
    fun existsByWishlistItem_IdAndTripMember_Id(wishlistItemId: Long, tripMemberId: Long): Boolean

    fun countByWishlistItem_Id(wishlistItemId: Long): Long

    @Modifying
    @Query(
        """
            delete from WishlistItemLike wil
            where wil.wishlistItem.id = :wishlistItemId and wil.tripMember.id = :tripMemberId
        """,
    )
    fun deleteByWishlistItemIdAndTripMemberId(
        @Param("wishlistItemId") wishlistItemId: Long,
        @Param("tripMemberId") tripMemberId: Long,
    ): Int

    @Modifying
    @Query("delete from WishlistItemLike wil where wil.tripMember.id = :tripMemberId")
    fun deleteByTripMemberId(@Param("tripMemberId") tripMemberId: Long): Int

    @Modifying
    @Query("delete from WishlistItemLike wil where wil.wishlistItem.id in :wishlistItemIds")
    fun deleteByWishlistItemIds(@Param("wishlistItemIds") wishlistItemIds: Collection<Long>): Int

    @Modifying
    @Query(
        """
            delete from WishlistItemLike wil
            where wil.wishlistItem.id in (
                select wi.id from WishlistItem wi
                where wi.adder.id = :adderId
            )
        """,
    )
    fun deleteByWishlistItemAdderId(@Param("adderId") adderId: Long): Int

    @Query(
        """
            select new com.tribe.domain.itinerary.wishlist.WishlistItemLikeCount(wil.wishlistItem.id, count(wil))
            from WishlistItemLike wil
            where wil.wishlistItem.id in :wishlistItemIds
            group by wil.wishlistItem.id
        """,
    )
    fun countByWishlistItemIds(
        @Param("wishlistItemIds") wishlistItemIds: Collection<Long>,
    ): List<WishlistItemLikeCount>

    @Query(
        """
            select wil.wishlistItem.id
            from WishlistItemLike wil
            where wil.tripMember.id = :tripMemberId and wil.wishlistItem.id in :wishlistItemIds
        """,
    )
    fun findLikedWishlistItemIds(
        @Param("tripMemberId") tripMemberId: Long,
        @Param("wishlistItemIds") wishlistItemIds: Collection<Long>,
    ): List<Long>
}
