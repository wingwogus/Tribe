package com.tribe.application.common.cursor

import java.time.LocalDateTime
import java.util.Base64

/**
 * 공통 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
object CursorCodec {
    data class Parsed(
        val createdAt: LocalDateTime,
        val id: Long,
    )

    fun encode(createdAt: LocalDateTime, id: Long): String {
        val raw = "${createdAt}|$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))
    }

    fun decode(cursor: String?): Parsed? {
        if (cursor.isNullOrBlank()) return null
        val decoded = runCatching {
            String(Base64.getUrlDecoder().decode(cursor), Charsets.UTF_8)
        }.getOrNull() ?: return null
        val parts = decoded.split("|", limit = 2)
        if (parts.size != 2) return null
        val createdAt = runCatching { LocalDateTime.parse(parts[0]) }.getOrNull() ?: return null
        val id = parts[1].toLongOrNull() ?: return null
        return Parsed(createdAt, id)
    }
}
