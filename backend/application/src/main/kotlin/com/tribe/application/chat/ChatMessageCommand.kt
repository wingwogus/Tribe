package com.tribe.application.chat

/**
 * 채팅 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object ChatMessageCommand {
    data class Send(
        val tripId: Long,
        val content: String,
    )
}
