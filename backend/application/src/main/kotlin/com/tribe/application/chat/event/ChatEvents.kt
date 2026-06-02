package com.tribe.application.chat.event

import java.time.LocalDateTime

/**
 * 채팅 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
enum class ChatEventType {
    MESSAGE,
    READ,
    UNREAD_COUNT,
}

data class ChatEvent(
    val type: ChatEventType,
    val roomId: Long,
    val message: ChatMessageEvent? = null,
    val read: ChatReadEvent? = null,
    val unread: ChatUnreadCountEvent? = null,
)

data class ChatMessageEvent(
    val id: Long,
    val roomId: Long,
    val senderId: Long,
    val content: String,
    val createdAt: LocalDateTime?,
)

data class ChatReadEvent(
    val readerId: Long,
    val lastReadMessageId: Long,
)

data class ChatUnreadCountEvent(
    val memberId: Long,
    val unreadCount: Long,
)
