package com.tribe.application.community

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.community.CommunityPost
import com.tribe.domain.community.CommunityPostRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.core.TripRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.Optional

class CommunityServiceTest {
    private val communityPostRepository = mockk<CommunityPostRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val tripRepository = mockk<TripRepository>()
    private val currentActor = mockk<CurrentActor>()
    private val tripAuthorizationPolicy = mockk<TripAuthorizationPolicy>()
    private val communityImageStorage = mockk<CommunityImageStorage>()

    @Test
    fun `listPosts maps repository rows into summaries`() {
        val now = LocalDateTime.of(2026, 4, 12, 12, 0)
        val author = Member(id = 5L, email = "user@example.com", passwordHash = "hash", nickname = "tribe")
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(2), Country.JAPAN)
        val post = CommunityPost(
            author = author,
            trip = trip,
            title = "title",
            content = "content",
            representativeImageUrl = null,
        )
        post.createdAt = now
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        every { communityPostRepository.findAll(pageable) } returns PageImpl(listOf(post), pageable, 1)

        val service = CommunityService(
            communityPostRepository,
            memberRepository,
            tripRepository,
            currentActor,
            tripAuthorizationPolicy,
            communityImageStorage,
        )

        val result = service.listPosts(CommunityQuery.ListPosts())

        assertEquals(1, result.size)
        assertEquals("title", result.first().title)
        assertEquals("tribe", result.first().authorNickname)
    }

    @Test
    fun `getPostDetail throws resource not found for missing post`() {
        every { communityPostRepository.findById(999L) } returns Optional.empty()

        val service = CommunityService(
            communityPostRepository,
            memberRepository,
            tripRepository,
            currentActor,
            tripAuthorizationPolicy,
            communityImageStorage,
        )

        val exception = assertThrows(BusinessException::class.java) {
            service.getPostDetail(CommunityQuery.GetPostDetail(999L))
        }

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `getPostDetail maps community post`() {
        val now = LocalDateTime.of(2026, 4, 12, 12, 0)
        val author = Member(id = 2L, email = "user@example.com", passwordHash = "hash", nickname = "tribe")
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(2), Country.JAPAN)
        val post = CommunityPost(
            author = author,
            trip = trip,
            title = "hello",
            content = "world",
            representativeImageUrl = "https://example.com/image.png",
        )
        post.createdAt = now

        every { communityPostRepository.findById(1L) } returns Optional.of(post)

        val service = CommunityService(
            communityPostRepository,
            memberRepository,
            tripRepository,
            currentActor,
            tripAuthorizationPolicy,
            communityImageStorage,
        )

        val result = service.getPostDetail(CommunityQuery.GetPostDetail(1L))

        assertEquals("hello", result.title)
        assertEquals("tribe", result.authorNickname)
        assertEquals("일본", result.country)
    }

    @Test
    fun `createPost uploads image and saves post`() {
        val author = Member(id = 7L, email = "user@example.com", passwordHash = "hash", nickname = "tribe")
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(2), Country.JAPAN)

        every { currentActor.requireUserId() } returns 7L
        every { tripAuthorizationPolicy.isTripAdmin(1L) } returns true
        every { memberRepository.findById(7L) } returns Optional.of(author)
        every { tripRepository.findById(1L) } returns Optional.of(trip)
        every { communityImageStorage.upload(any(), "community") } returns "https://example.com/image.png"
        every { communityPostRepository.save(any()) } answers { firstArg() }

        val file = org.springframework.mock.web.MockMultipartFile("image", "a.png", "image/png", byteArrayOf(1, 2, 3))
        val service = CommunityService(
            communityPostRepository,
            memberRepository,
            tripRepository,
            currentActor,
            tripAuthorizationPolicy,
            communityImageStorage,
        )

        val result = service.createPost(
            CommunityQuery.CreatePost(1L, "title", "content"),
            file,
        )

        assertEquals("title", result.title)
        assertEquals("https://example.com/image.png", result.representativeImageUrl)
    }
}
