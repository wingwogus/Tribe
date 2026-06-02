package com.tribe.application.redis

import java.time.Duration

/**
 * Redis 저장소 application 저장소 port 경계.
 *
 * Redis/외부 저장소 접근 의도를 use case 언어로 분리.
 */
interface TripInvitationRepository {
    fun save(token: String, tripId: Long, ttl: Duration)
    fun getTripId(token: String): Long?
}
