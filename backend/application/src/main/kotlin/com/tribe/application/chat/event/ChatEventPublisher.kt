package com.tribe.application.chat.event

/**
 * 채팅 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
interface ChatEventPublisher {
    fun publish(event: ChatEvent)
}
