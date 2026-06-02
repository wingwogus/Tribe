package com.tribe.api.chat

import com.tribe.application.chat.ChatMessageCommand

/**
 * 채팅 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object ChatMessageRequests {
    data class SendRequest(
        val content: String,
    ) {
        fun toCommand(tripId: Long): ChatMessageCommand.Send = ChatMessageCommand.Send(
            tripId = tripId,
            content = content,
        )
    }
}
