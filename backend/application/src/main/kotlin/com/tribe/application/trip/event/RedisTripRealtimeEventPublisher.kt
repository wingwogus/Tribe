package com.tribe.application.trip.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 여행 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
@Component
@ConditionalOnProperty(name = ["trip.realtime.enabled"], havingValue = "true")
class RedisTripRealtimeEventPublisher(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : TripRealtimeEventPublisher {

    companion object {
        const val CHANNEL = "tribe:trip-events"
    }

    override fun publish(event: TripRealtimeEvent) {
        redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event))
    }
}
