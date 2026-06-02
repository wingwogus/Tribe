package com.tribe.api.community

import com.tribe.application.community.CommunityResult
import java.time.LocalDateTime

/**
 * 커뮤니티 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object CommunityResponses {
    data class PostListResponse(
        val posts: List<PostSummaryResponse>
    ) {
        companion object {
            fun from(posts: List<CommunityResult.PostSummary>): PostListResponse {
                return PostListResponse(posts.map(PostSummaryResponse::from))
            }
        }
    }

    data class PostSummaryResponse(
        val id: Long,
        val title: String,
        val authorId: Long,
        val authorNickname: String,
        val country: String,
        val representativeImageUrl: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime?,
    ) {
        companion object {
            fun from(post: CommunityResult.PostSummary): PostSummaryResponse {
                return PostSummaryResponse(
                    id = post.id,
                    title = post.title,
                    authorId = post.authorId,
                    authorNickname = post.authorNickname,
                    country = post.country,
                    representativeImageUrl = post.representativeImageUrl,
                    createdAt = post.createdAt,
                    updatedAt = post.updatedAt
                )
            }
        }
    }

    data class PostDetailResponse(
        val id: Long,
        val title: String,
        val content: String,
        val authorId: Long,
        val authorNickname: String,
        val country: String,
        val representativeImageUrl: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime?,
    ) {
        companion object {
            fun from(post: CommunityResult.PostDetail): PostDetailResponse {
                return PostDetailResponse(
                    id = post.id,
                    title = post.title,
                    content = post.content,
                    authorId = post.authorId,
                    authorNickname = post.authorNickname,
                    country = post.country,
                    representativeImageUrl = post.representativeImageUrl,
                    createdAt = post.createdAt,
                    updatedAt = post.updatedAt,
                )
            }
        }
    }
}
