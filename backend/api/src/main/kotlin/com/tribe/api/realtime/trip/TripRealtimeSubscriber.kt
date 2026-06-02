package com.tribe.api.realtime.trip

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.trip.event.TripRealtimeEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * 여행 API 계층 경계.
 *
 * HTTP transport와 application 계층 분리.
 */
@Component
@ConditionalOnProperty(name = ["trip.realtime.enabled"], havingValue = "true")
class TripRealtimeSubscriber(
    private val objectMapper: ObjectMapper,
    private val messagingTemplate: SimpMessagingTemplate,
) : MessageListener {
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val payload = message.body.toString(Charsets.UTF_8)
        val event = objectMapper.readValue(payload, TripRealtimeEvent::class.java)
        messagingTemplate.convertAndSend("/sub/trips/${event.tripId}", event)
    }
}
