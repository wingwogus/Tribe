package com.tribe.application.chat

import com.tribe.application.common.cursor.CursorCodec
import com.tribe.application.chat.event.ChatEvent
import com.tribe.application.chat.event.ChatEventType
import com.tribe.application.chat.event.ChatMessageEvent
import com.tribe.domain.chat.ChatMessage

/**
 * 채팅 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object ChatMessageResult {
    data class Sender(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val avatar: String?,
    )

    data class Message(
        val messageId: Long,
        val sender: Sender,
        val content: String,
        val timestamp: String,
    ) {
        companion object {
            fun from(chatMessage: ChatMessage): Message {
                return Message(
                    messageId = chatMessage.id,
                    sender = Sender(
                        tripMemberId = chatMessage.sender.id,
                        memberId = chatMessage.sender.member?.id,
                        nickname = chatMessage.sender.name,
                        avatar = chatMessage.sender.member?.avatar,
                    ),
                    content = chatMessage.content,
                    timestamp = chatMessage.createdAt.toString(),
                )
            }
        }
    }

    data class History(
        val content: List<Message>,
        val nextCursor: String?,
        val hasNext: Boolean,
    )

    fun toEvent(message: Message, tripId: Long): ChatEvent {
        return ChatEvent(
            type = ChatEventType.MESSAGE,
            roomId = tripId,
            message = ChatMessageEvent(
                id = message.messageId,
                roomId = tripId,
                senderId = message.sender.memberId ?: 0L,
                content = message.content,
                createdAt = java.time.LocalDateTime.parse(message.timestamp),
            ),
        )
    }
}
