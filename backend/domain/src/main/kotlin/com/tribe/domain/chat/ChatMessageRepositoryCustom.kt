package com.tribe.domain.chat

import java.time.LocalDateTime

interface ChatMessageRepositoryCustom {
    fun findHistoryPage(
        tripId: Long,
        cursorCreatedAt: LocalDateTime?,
        cursorId: Long?,
        limit: Int,
    ): List<ChatMessage>
}
