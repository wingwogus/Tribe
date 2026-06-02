package com.tribe.application.chat

/**
 * 채팅 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
object ChatMessageQuery {
    data class History(
        val tripId: Long,
        val cursor: String?,
        val pageSize: Int,
    )
}
