package com.tribe.application.redis

import java.time.Duration

/**
 * Redis 저장소 application 저장소 port 경계.
 *
 * Redis/외부 저장소 접근 의도를 use case 언어로 분리.
 */
interface EmailVerificationRepository {
    fun saveCode(email: String, code: String, ttl: Duration)

    fun getCode(email: String): String?

    fun markVerified(email: String, ttl: Duration)

    fun isVerified(email: String): Boolean

    fun deleteCode(email: String)

    fun deleteVerified(email: String)
}
