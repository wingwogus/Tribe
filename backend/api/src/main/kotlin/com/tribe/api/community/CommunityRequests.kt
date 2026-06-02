package com.tribe.api.community

import com.tribe.application.community.CommunityCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * 커뮤니티 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object CommunityRequests {
    data class CreatePostRequest(
        @field:NotNull(message = "공유할 여행 ID는 필수입니다.")
        val tripId: Long,
        @field:NotBlank(message = "게시글 제목은 필수입니다.")
        val title: String,
        @field:NotBlank(message = "게시글 내용은 필수입니다.")
        val content: String,
    ) {
        fun toCommand(): CommunityCommand.CreatePost = CommunityCommand.CreatePost(
            tripId = tripId,
            title = title,
            content = content,
        )
    }

    data class UpdatePostRequest(
        @field:NotBlank(message = "게시글 제목은 필수입니다.")
        val title: String,
        @field:NotBlank(message = "게시글 내용은 필수입니다.")
        val content: String,
    ) {
        fun toCommand(postId: Long): CommunityCommand.UpdatePost = CommunityCommand.UpdatePost(
            postId = postId,
            title = title,
            content = content,
        )
    }

    data class ListPostsRequest(
        @field:Min(0, message = "페이지는 0 이상이어야 합니다")
        val page: Int = 0,

        @field:Min(1, message = "조회 개수는 1 이상이어야 합니다")
        @field:Max(100, message = "조회 개수는 100 이하여야 합니다")
        val size: Int = 20,
        val country: String? = null,
        val authorId: Long? = null,
    ) {
        fun toCommand(): CommunityCommand.ListPosts = CommunityCommand.ListPosts(
            page = page,
            size = size,
            country = country,
            authorId = authorId,
        )
    }
}
