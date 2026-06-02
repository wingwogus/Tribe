package com.tribe.application.itinerary.place

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

/**
 * 장소 검색 Redis cache adapter.
 *
 * 외부 검색 후보 목록을 짧게 보관하고 canonical 병합은 조회 시점에 재수행.
 */
@Repository
class RedisPlaceSearchCacheRepository(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : PlaceSearchCacheRepository {
    companion object {
        private const val PREFIX = "place-search:"
    }

    override fun get(key: String): List<PlaceSearchGateway.SearchHit>? {
        // cache payload는 SearchHit 후보만 포함, 내부 placeId는 저장하지 않는 구조.
        val payload = redis.opsForValue().get(PREFIX + key) ?: return null
        return objectMapper.readValue(payload, object : TypeReference<List<PlaceSearchGateway.SearchHit>>() {})
    }

    override fun put(key: String, value: List<PlaceSearchGateway.SearchHit>, ttl: Duration) {
        // TTL은 use case에서 결정, adapter는 Redis key prefix와 직렬화만 담당.
        redis.opsForValue().set(PREFIX + key, objectMapper.writeValueAsString(value), ttl)
    }
}
