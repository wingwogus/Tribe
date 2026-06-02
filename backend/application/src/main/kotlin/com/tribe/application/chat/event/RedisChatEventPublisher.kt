package com.tribe.application.chat.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 채팅 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
@Component
@ConditionalOnProperty(name = ["chat.redis.enabled"], havingValue = "true")
class RedisChatEventPublisher(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : ChatEventPublisher {

    companion object {
        const val CHANNEL = "tribe:chat-events"
    }

    override fun publish(event: ChatEvent) {
        redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event))
    }
}
