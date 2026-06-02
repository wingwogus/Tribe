package com.tribe.application.chat.event

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 채팅 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
@Component
@ConditionalOnProperty(name = ["chat.redis.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpChatEventPublisher : ChatEventPublisher {
    override fun publish(event: ChatEvent) {
        // Tests can disable Redis and still exercise chat behavior without a broker connection.
    }
}
