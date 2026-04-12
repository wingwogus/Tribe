package com.tribe.domain.community

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.tribe.domain.community.QCommunityPost.communityPost
import com.tribe.domain.trip.core.Country
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class CommunityPostRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CommunityPostRepositoryCustom {

    private fun <T> JPAQuery<T>.limitPage(pageSize: Int): JPAQuery<T> = limit(pageSize.toLong())

    override fun searchPost(condition: PostSearchCondition, pageable: Pageable): Page<CommunityPost> {
        val content = queryFactory
            .selectFrom(communityPost)
            .join(communityPost.author).fetchJoin()
            .join(communityPost.trip).fetchJoin()
            .where(
                countryEq(condition.country),
                authorIdEq(condition.authorId),
            )
            .orderBy(communityPost.createdAt.desc())
            .offset(pageable.offset)
            .limitPage(pageable.pageSize)
            .fetch()

        val countQuery = queryFactory
            .select(communityPost.count())
            .from(communityPost)
            .where(
                countryEq(condition.country),
                authorIdEq(condition.authorId),
            )

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    private fun countryEq(country: String?): BooleanExpression? {
        return country
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Country.from(it) }.getOrNull() }
            ?.let { communityPost.trip.country.eq(it) }
    }

    private fun authorIdEq(authorId: Long?): BooleanExpression? {
        return authorId?.let { communityPost.author.id.eq(it) }
    }
}
