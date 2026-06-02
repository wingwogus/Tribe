package com.tribe.api.chat

import com.tribe.application.chat.ChatMessageResult

/**
 * 채팅 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object ChatMessageResponses {
    data class MessageResponse(
        val messageId: Long,
        val sender: SenderResponse,
        val content: String,
        val timestamp: String,
    ) {
        companion object {
            fun from(message: ChatMessageResult.Message) = MessageResponse(
                messageId = message.messageId,
                sender = SenderResponse.from(message.sender),
                content = message.content,
                timestamp = message.timestamp,
            )
        }
    }

    data class SenderResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val avatar: String?,
    ) {
        companion object {
            fun from(sender: ChatMessageResult.Sender) = SenderResponse(
                tripMemberId = sender.tripMemberId,
                memberId = sender.memberId,
                nickname = sender.nickname,
                avatar = sender.avatar,
            )
        }
    }

    data class HistoryResponse(
        val content: List<MessageResponse>,
        val nextCursor: String?,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(history: ChatMessageResult.History) = HistoryResponse(
                content = history.content.map(MessageResponse::from),
                nextCursor = history.nextCursor,
                hasNext = history.hasNext,
            )
        }
    }
}
