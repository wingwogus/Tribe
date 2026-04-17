package com.tribe.domain.chat

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.tribe.domain.chat.QChatMessage.chatMessage
import com.tribe.domain.member.QMember.member
import com.tribe.domain.trip.member.QTripMember.tripMember

class ChatMessageRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ChatMessageRepositoryCustom {

    override fun findHistoryPage(
        tripId: Long,
        cursorCreatedAt: java.time.LocalDateTime?,
        cursorId: Long?,
        limit: Int,
    ): List<ChatMessage> {
        return queryFactory
            .selectFrom(chatMessage)
            .join(chatMessage.sender, tripMember).fetchJoin()
            .leftJoin(tripMember.member, member).fetchJoin()
            .where(
                chatMessage.trip.id.eq(tripId),
                cursorCondition(cursorCreatedAt, cursorId),
            )
            .orderBy(chatMessage.createdAt.desc(), chatMessage.id.desc())
            .limit(limit.toLong())
            .fetch()
    }

    private fun cursorCondition(
        cursorCreatedAt: java.time.LocalDateTime?,
        cursorId: Long?,
    ): BooleanExpression? {
        if (cursorCreatedAt == null || cursorId == null) {
            return null
        }

        return chatMessage.createdAt.lt(cursorCreatedAt)
            .or(
                chatMessage.createdAt.eq(cursorCreatedAt)
                    .and(chatMessage.id.lt(cursorId))
            )
    }
}
