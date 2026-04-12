package com.tribe.application.chat.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations

class RedisChatEventPublisherTest {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun `publish sends serialized chat event to redis channel`() {
        val redis = mockk<StringRedisTemplate>()
        val publisher = RedisChatEventPublisher(redis, objectMapper)
        val event = ChatEvent(
            type = ChatEventType.MESSAGE,
            roomId = 7L,
            message = ChatMessageEvent(1L, 7L, 2L, "hello", null),
        )

        every { redis.convertAndSend(any(), any<String>()) } returns 1L

        publisher.publish(event)

        verify(exactly = 1) {
            redis.convertAndSend(
                RedisChatEventPublisher.CHANNEL,
                objectMapper.writeValueAsString(event),
            )
        }
    }
}
