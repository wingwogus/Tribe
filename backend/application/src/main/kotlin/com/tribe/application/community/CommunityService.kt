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
    fun createPost(query: CommunityQuery.CreatePost, imageFile: MultipartFile?): CommunityPostDetail {
        tripAuthorizationPolicy.isTripAdmin(query.tripId)
        val author = memberRepository.findById(currentActor.requireUserId())
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val trip = tripRepository.findById(query.tripId)
            .orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }

        val imageUrl = if (imageFile != null && !imageFile.isEmpty) {
            communityImageStorage.upload(imageFile, "community")
        } else null

        val post = communityPostRepository.save(
            CommunityPost(
                author = author,
                trip = trip,
                title = query.title,
                content = query.content,
                representativeImageUrl = imageUrl,
            )
        )

        return post.toDetail()
    }

    fun listPosts(query: CommunityQuery.ListPosts): List<CommunityPostSummary> {
        val pageable = PageRequest.of(
            query.page.coerceAtLeast(0),
            query.size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val posts = if (query.country != null || query.authorId != null) {
            communityPostRepository.searchPost(PostSearchCondition(query.country, query.authorId), pageable)
        } else {
            communityPostRepository.findAll(pageable)
        }

        return posts
            .content
            .map { it.toSummary() }
    }

    fun getPostDetail(query: CommunityQuery.GetPostDetail): CommunityPostDetail {
        val post = communityPostRepository.findById(query.postId)
            .orElseThrow {
                BusinessException(
                    errorCode = ErrorCode.RESOURCE_NOT_FOUND,
                    detail = mapOf("postId" to query.postId)
                )
            }

        return CommunityPostDetail(
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

    fun updatePost(query: CommunityQuery.UpdatePost, imageFile: MultipartFile?): CommunityPostDetail {
        val post = communityPostRepository.findByIdWithDetails(query.postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        tripAuthorizationPolicy.isTripOwner(post.trip.id)

        post.title = query.title
        post.content = query.content

        if (imageFile != null && !imageFile.isEmpty) {
            val oldImageUrl = post.representativeImageUrl
            post.representativeImageUrl = communityImageStorage.upload(imageFile, "community")
            if (oldImageUrl != null) {
                communityImageStorage.delete(oldImageUrl)
            }
        }

        return post.toDetail()
    }

    fun deletePost(query: CommunityQuery.DeletePost) {
        val post = communityPostRepository.findByIdWithDetails(query.postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        tripAuthorizationPolicy.isTripOwner(post.trip.id)
        val oldImageUrl = post.representativeImageUrl
        communityPostRepository.delete(post)
        if (oldImageUrl != null) {
            communityImageStorage.delete(oldImageUrl)
        }
    }

    private fun CommunityPost.toSummary(): CommunityPostSummary {
        return CommunityPostSummary(
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

    private fun CommunityPost.toDetail(): CommunityPostDetail {
        return CommunityPostDetail(
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
