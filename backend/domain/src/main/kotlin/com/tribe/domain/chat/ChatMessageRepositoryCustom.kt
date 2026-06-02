package com.tribe.domain.chat

import java.time.LocalDateTime

/**
 * 채팅 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface ChatMessageRepositoryCustom {
    fun findHistoryPage(
        tripId: Long,
        cursorCreatedAt: LocalDateTime?,
        cursorId: Long?,
        limit: Int,
    ): List<ChatMessage>
}
