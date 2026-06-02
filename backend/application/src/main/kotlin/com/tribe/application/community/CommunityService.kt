package com.tribe.application.community

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.community.CommunityPost
import com.tribe.domain.community.CommunityPostRepository
import com.tribe.domain.community.PostSearchCondition
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.TripRepository
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.core.TripAuthorizationPolicy
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 커뮤니티 use case 경계.
 *
 * 검증, 도메인 조회, 결과 조립 순서 보관.
 */
@Service
@Transactional
class CommunityService(
    private val communityPostRepository: CommunityPostRepository,
    private val memberRepository: MemberRepository,
    private val tripRepository: TripRepository,
    private val currentActor: CurrentActor,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
    private val communityImageStorage: CommunityImageStorage,
) {
    fun createPost(command: CommunityCommand.CreatePost, imageFile: MultipartFile?): CommunityResult.PostDetail {
        tripAuthorizationPolicy.isTripAdmin(command.tripId)
        val author = memberRepository.findById(currentActor.requireUserId())
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val trip = tripRepository.findById(command.tripId)
            .orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }

        val imageUrl = if (imageFile != null && !imageFile.isEmpty) {
            communityImageStorage.upload(imageFile, "community")
        } else null

        val post = communityPostRepository.save(
            CommunityPost(
                author = author,
                trip = trip,
                title = command.title,
                content = command.content,
                representativeImageUrl = imageUrl,
            )
        )

        return post.toDetail()
    }

    fun listPosts(command: CommunityCommand.ListPosts): List<CommunityResult.PostSummary> {
        val pageable = PageRequest.of(
            command.page.coerceAtLeast(0),
            command.size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val posts = if (command.country != null || command.authorId != null) {
            communityPostRepository.searchPost(PostSearchCondition(command.country, command.authorId), pageable)
        } else {
            communityPostRepository.findAll(pageable)
        }

        return posts
            .content
            .map { it.toSummary() }
    }

    fun getPostDetail(command: CommunityCommand.GetPostDetail): CommunityResult.PostDetail {
        val post = communityPostRepository.findById(command.postId)
            .orElseThrow {
                BusinessException(
                    errorCode = ErrorCode.RESOURCE_NOT_FOUND,
                    detail = mapOf("postId" to command.postId)
                )
            }

        return CommunityResult.PostDetail(
            id = post.id,
            title = post.title,
            content = post.content,
            authorId = post.author.id,
            authorNickname = post.author.nickname,
            country = post.trip.country.koreanName,
            representativeImageUrl = post.representativeImageUrl,
            createdAt = post.createdAt,
            updatedAt = null,
        )
    }

    fun updatePost(command: CommunityCommand.UpdatePost, imageFile: MultipartFile?): CommunityResult.PostDetail {
        val post = communityPostRepository.findByIdWithDetails(command.postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        tripAuthorizationPolicy.isTripOwner(post.trip.id)

        post.title = command.title
        post.content = command.content

        if (imageFile != null && !imageFile.isEmpty) {
            val oldImageUrl = post.representativeImageUrl
            post.representativeImageUrl = communityImageStorage.upload(imageFile, "community")
            if (oldImageUrl != null) {
                communityImageStorage.delete(oldImageUrl)
            }
        }

        return post.toDetail()
    }

    fun deletePost(command: CommunityCommand.DeletePost) {
        val post = communityPostRepository.findByIdWithDetails(command.postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        tripAuthorizationPolicy.isTripOwner(post.trip.id)
        val oldImageUrl = post.representativeImageUrl
        communityPostRepository.delete(post)
        if (oldImageUrl != null) {
            communityImageStorage.delete(oldImageUrl)
        }
    }

    private fun CommunityPost.toSummary(): CommunityResult.PostSummary {
        return CommunityResult.PostSummary(
            id = id,
            title = title,
            authorId = author.id,
            authorNickname = author.nickname,
            country = trip.country.koreanName,
            representativeImageUrl = representativeImageUrl,
            createdAt = createdAt,
            updatedAt = null,
        )
    }

    private fun CommunityPost.toDetail(): CommunityResult.PostDetail {
        return CommunityResult.PostDetail(
            id = id,
            title = title,
            content = content,
            authorId = author.id,
            authorNickname = author.nickname,
            country = trip.country.koreanName,
            representativeImageUrl = representativeImageUrl,
            createdAt = createdAt,
            updatedAt = null,
        )
    }
}
