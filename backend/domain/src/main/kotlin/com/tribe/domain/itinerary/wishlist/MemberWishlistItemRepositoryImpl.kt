package com.tribe.domain.itinerary.wishlist

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.tribe.domain.itinerary.place.QPlace.place
import com.tribe.domain.itinerary.place.QPlaceDetailSnapshot.placeDetailSnapshot
import com.tribe.domain.itinerary.wishlist.QMemberWishlistItem.memberWishlistItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class MemberWishlistItemRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : MemberWishlistItemRepositoryCustom {
    private fun <T> JPAQuery<T>.limitPage(pageable: Pageable): JPAQuery<T> =
        offset(pageable.offset).limit(pageable.pageSize.toLong())

    override fun findPageByMember(
        memberId: Long,
        query: String?,
        sort: AccountWishlistSort?,
        pageable: Pageable,
    ): Page<MemberWishlistItem> {
        val content = queryFactory
            .selectFrom(memberWishlistItem)
            .join(memberWishlistItem.place, place).fetchJoin()
            .leftJoin(place.detailSnapshot, placeDetailSnapshot).fetchJoin()
            .where(
                memberWishlistItem.member.id.eq(memberId),
                placeNameContains(query),
            )
            .orderBy(*orderSpecifiers(sort))
            .limitPage(pageable)
            .fetch()

        val countQuery = queryFactory
            .select(memberWishlistItem.count())
            .from(memberWishlistItem)
            .join(memberWishlistItem.place, place)
            .where(
                memberWishlistItem.member.id.eq(memberId),
                placeNameContains(query),
            )

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    private fun placeNameContains(query: String?): BooleanExpression? =
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { place.name.containsIgnoreCase(it) }

    private fun orderSpecifiers(sort: AccountWishlistSort?): Array<OrderSpecifier<*>> =
        when (sort) {
            AccountWishlistSort.RATING_DESC -> arrayOf(
                placeDetailSnapshot.rating.coalesce(-1.0).desc(),
                memberWishlistItem.id.desc(),
            )
            AccountWishlistSort.REVIEW_COUNT_DESC -> arrayOf(
                placeDetailSnapshot.userRatingCount.coalesce(0).desc(),
                memberWishlistItem.id.desc(),
            )
            AccountWishlistSort.REVIEW_GOOD_DESC -> arrayOf(
                placeDetailSnapshot.rating.coalesce(-1.0).desc(),
                placeDetailSnapshot.userRatingCount.coalesce(0).desc(),
                memberWishlistItem.id.desc(),
            )
            null -> arrayOf(memberWishlistItem.id.desc())
        }
}
