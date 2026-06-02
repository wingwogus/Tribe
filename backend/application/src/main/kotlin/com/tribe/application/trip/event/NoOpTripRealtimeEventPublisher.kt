package com.tribe.application.trip.event

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 여행 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
@Component
@ConditionalOnProperty(name = ["trip.realtime.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpTripRealtimeEventPublisher : TripRealtimeEventPublisher {
    override fun publish(event: TripRealtimeEvent) {
        // local/test can disable trip realtime transport while preserving mutation behavior
    }
}
