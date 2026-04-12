package com.tribe.api.realtime.trip

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.trip.core.TripResult
import com.tribe.application.trip.event.TripMemberAction
import com.tribe.application.trip.event.TripMemberEvent
import com.tribe.application.trip.event.TripRealtimeEvent
import com.tribe.application.trip.event.TripRealtimeEventType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.Message
import org.springframework.messaging.simp.SimpMessagingTemplate

class TripRealtimeSubscriberTest {
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val messagingTemplate = mockk<SimpMessagingTemplate>()
    private val subscriber = TripRealtimeSubscriber(objectMapper, messagingTemplate)

    @Test
    fun `trip realtime event is broadcast to trip topic`() {
        every { messagingTemplate.convertAndSend(any<String>(), any<Any>()) } just runs

        val event = TripRealtimeEvent(
            type = TripRealtimeEventType.TRIP_MEMBER,
            tripId = 11L,
            actorId = 2L,
            member = TripMemberEvent(
                action = TripMemberAction.MEMBER_LEFT,
                member = TripResult.MemberSummary(
                    tripMemberId = 21L,
                    memberId = 2L,
                    nickname = "member",
                    role = "EXITED",
                ),
            ),
        )
        val message = mockk<Message> {
            every { body } returns objectMapper.writeValueAsBytes(event)
        }

        subscriber.onMessage(message, null)

        verify {
            messagingTemplate.convertAndSend("/sub/trips/11", event)
        }
    }
}
