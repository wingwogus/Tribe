package com.tribe.application.redis

/**
 * Redis 저장소 application 저장소 port 경계.
 *
 * Redis/외부 저장소 접근 의도를 use case 언어로 분리.
 */
interface RefreshTokenRepository {
    fun save(userId: Long, refreshToken: String, expiresInSeconds: Long)

    fun get(userId: Long): String?

    fun delete(userId: Long)
}