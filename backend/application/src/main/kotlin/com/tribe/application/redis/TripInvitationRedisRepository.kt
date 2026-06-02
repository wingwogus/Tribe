package com.tribe.application.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

/**
 * Redis 저장소 application 저장소 port 경계.
 *
 * Redis/외부 저장소 접근 의도를 use case 언어로 분리.
 */
@Repository
class TripInvitationRedisRepository(
    private val redis: StringRedisTemplate,
) : TripInvitationRepository {
    companion object {
        private const val PREFIX = "invite:"
    }

    override fun save(token: String, tripId: Long, ttl: Duration) {
        redis.opsForValue().set(PREFIX + token, tripId.toString(), ttl)
    }

    override fun getTripId(token: String): Long? {
        return redis.opsForValue().get(PREFIX + token)?.toLongOrNull()
    }
}
