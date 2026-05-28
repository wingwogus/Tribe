package com.tribe.domain.itinerary.wishlist

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.tribe.domain.itinerary.place.QPlace.place
import com.tribe.domain.itinerary.place.QPlaceDetailSnapshot.placeDetailSnapshot
import com.tribe.domain.itinerary.wishlist.QWishlistItem.wishlistItem
import com.tribe.domain.itinerary.wishlist.QWishlistItemLike.wishlistItemLike
import com.tribe.domain.member.QMember.member
import com.tribe.domain.trip.member.QTripMember.tripMember
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class WishlistItemRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : WishlistItemRepositoryCustom {
    private fun <T> JPAQuery<T>.limitPage(pageable: Pageable): JPAQuery<T> =
        offset(pageable.offset).limit(pageable.pageSize.toLong())

    override fun findPageByTrip(
        tripId: Long,
        query: String?,
        sort: TripWishlistSort?,
        pageable: Pageable,
    ): Page<WishlistItem> {
        val content = if (sort == TripWishlistSort.LIKE_COUNT_DESC || sort == TripWishlistSort.LIKE_COUNT_ASC) {
            findPageContentByTripOrderByLikeCount(tripId, query, sort, pageable)
        } else {
            findPageContentByTrip(tripId, query, sort, pageable)
        }

        val countQuery = queryFactory
            .select(wishlistItem.count())
            .from(wishlistItem)
            .join(wishlistItem.place, place)
            .where(
                wishlistItem.trip.id.eq(tripId),
                placeNameContains(query),
            )

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    private fun findPageContentByTrip(
        tripId: Long,
        query: String?,
        sort: TripWishlistSort?,
        pageable: Pageable,
    ): List<WishlistItem> =
        queryFactory
            .selectFrom(wishlistItem)
            .join(wishlistItem.place, place).fetchJoin()
            .leftJoin(place.detailSnapshot, placeDetailSnapshot).fetchJoin()
            .join(wishlistItem.adder, tripMember).fetchJoin()
            .leftJoin(tripMember.member, member).fetchJoin()
            .where(
                wishlistItem.trip.id.eq(tripId),
                placeNameContains(query),
            )
            .orderBy(*orderSpecifiers(sort))
            .limitPage(pageable)
            .fetch()

    private fun findPageContentByTripOrderByLikeCount(
        tripId: Long,
        query: String?,
        sort: TripWishlistSort,
        pageable: Pageable,
    ): List<WishlistItem> {
        val orderedIds = queryFactory
            .select(wishlistItem.id)
            .from(wishlistItem)
            .join(wishlistItem.place, place)
            .leftJoin(wishlistItemLike).on(wishlistItemLike.wishlistItem.eq(wishlistItem))
            .where(
                wishlistItem.trip.id.eq(tripId),
                placeNameContains(query),
            )
            .groupBy(wishlistItem.id)
            .orderBy(likeCountOrderSpecifier(sort), wishlistItem.id.desc())
            .limitPage(pageable)
            .fetch()

        if (orderedIds.isEmpty()) return emptyList()

        val orderById = orderedIds.withIndex().associate { it.value to it.index }
        return queryFactory
            .selectFrom(wishlistItem)
            .join(wishlistItem.place, place).fetchJoin()
            .leftJoin(place.detailSnapshot, placeDetailSnapshot).fetchJoin()
            .join(wishlistItem.adder, tripMember).fetchJoin()
            .leftJoin(tripMember.member, member).fetchJoin()
            .where(wishlistItem.id.`in`(orderedIds))
            .fetch()
            .sortedBy { orderById[it.id] ?: Int.MAX_VALUE }
    }

    private fun likeCountOrderSpecifier(sort: TripWishlistSort): OrderSpecifier<Long> =
        OrderSpecifier(
            if (sort == TripWishlistSort.LIKE_COUNT_ASC) Order.ASC else Order.DESC,
            wishlistItemLike.count(),
        )

    private fun placeNameContains(query: String?): BooleanExpression? =
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { place.name.containsIgnoreCase(it) }

    private fun orderSpecifiers(sort: TripWishlistSort?): Array<OrderSpecifier<*>> =
        when (sort) {
            TripWishlistSort.RATING_DESC -> arrayOf(
                placeDetailSnapshot.rating.coalesce(-1.0).desc(),
                wishlistItem.id.desc(),
            )
            TripWishlistSort.REVIEW_COUNT_DESC -> arrayOf(
                placeDetailSnapshot.userRatingCount.coalesce(0).desc(),
                wishlistItem.id.desc(),
            )
            TripWishlistSort.REVIEW_GOOD_DESC -> arrayOf(
                placeDetailSnapshot.rating.coalesce(-1.0).desc(),
                placeDetailSnapshot.userRatingCount.coalesce(0).desc(),
                wishlistItem.id.desc(),
            )
            null -> arrayOf(wishlistItem.id.desc())
            TripWishlistSort.LIKE_COUNT_DESC,
            TripWishlistSort.LIKE_COUNT_ASC,
            -> arrayOf(wishlistItem.id.desc())
        }
}
