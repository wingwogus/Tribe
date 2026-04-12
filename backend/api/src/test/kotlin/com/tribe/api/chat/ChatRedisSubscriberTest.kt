package com.tribe.api.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.chat.event.ChatEvent
import com.tribe.application.chat.event.ChatEventType
import com.tribe.application.chat.event.ChatMessageEvent
import com.tribe.application.chat.event.ChatUnreadCountEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.Message
import org.springframework.messaging.simp.SimpMessagingTemplate

class ChatRedisSubscriberTest {
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val messagingTemplate = mockk<SimpMessagingTemplate>()
    private val subscriber = ChatRedisSubscriber(objectMapper, messagingTemplate)

    @Test
    fun `message event is broadcast to room topic`() {
        every { messagingTemplate.convertAndSend(any<String>(), any<Any>()) } just runs

        val event = ChatEvent(
            type = ChatEventType.MESSAGE,
            roomId = 11L,
            message = ChatMessageEvent(1L, 11L, 2L, "hello", null),
        )
        val message = mockk<Message> {
            every { body } returns objectMapper.writeValueAsBytes(event)
        }

        subscriber.onMessage(message, null)

        verify {
            messagingTemplate.convertAndSend("/sub/chat/rooms/11", event)
        }
    }

    @Test
    fun `unread count event is sent to user queue`() {
        every { messagingTemplate.convertAndSendToUser(any(), any(), any<Any>()) } just runs

        val event = ChatEvent(
            type = ChatEventType.UNREAD_COUNT,
            roomId = 12L,
            unread = ChatUnreadCountEvent(memberId = 5L, unreadCount = 3L),
        )
        val message = mockk<Message> {
            every { body } returns objectMapper.writeValueAsBytes(event)
        }

        subscriber.onMessage(message, null)

        verify {
            messagingTemplate.convertAndSendToUser("5", "/queue/unread", event)
        }
    }
}
