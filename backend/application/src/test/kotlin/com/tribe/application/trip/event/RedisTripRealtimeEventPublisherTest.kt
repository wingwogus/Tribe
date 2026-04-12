package com.tribe.application.trip.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.trip.core.TripResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate

class RedisTripRealtimeEventPublisherTest {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun `publish sends serialized realtime event to redis channel`() {
        val redis = mockk<StringRedisTemplate>()
        val publisher = RedisTripRealtimeEventPublisher(redis, objectMapper)
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

        every { redis.convertAndSend(any(), any<String>()) } returns 1L

        publisher.publish(event)

        verify(exactly = 1) {
            redis.convertAndSend(
                RedisTripRealtimeEventPublisher.CHANNEL,
                objectMapper.writeValueAsString(event),
            )
        }
    }
}
