package com.tribe.application.itinerary.place

import java.time.Duration

/**
 * 장소 application 저장소 port 경계.
 *
 * Redis/외부 저장소 접근 의도를 use case 언어로 분리.
 */
interface PlaceSearchCacheRepository {
    fun get(key: String): List<PlaceSearchGateway.SearchHit>?
    fun put(key: String, value: List<PlaceSearchGateway.SearchHit>, ttl: Duration)
}
