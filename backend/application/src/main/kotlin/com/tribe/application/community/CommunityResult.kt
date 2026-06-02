package com.tribe.application.community

import java.time.LocalDateTime

/**
 * 커뮤니티 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object CommunityResult {
    data class PostSummary(
        val id: Long,
        val title: String,
        val authorId: Long,
        val authorNickname: String,
        val country: String,
        val representativeImageUrl: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime?,
    )

    data class PostDetail(
        val id: Long,
        val title: String,
        val content: String,
        val authorId: Long,
        val authorNickname: String,
        val country: String,
        val representativeImageUrl: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime?,
    )
}
