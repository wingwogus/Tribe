package com.tribe.application.chat

import com.tribe.application.chat.event.ChatEventPublisher
import com.tribe.application.common.cursor.CursorCodec
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.chat.ChatMessage
import com.tribe.domain.chat.ChatMessageRepository
import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ChatMessageServiceTest {
    private val currentActor = mockk<CurrentActor>()
    private val tripAuthorizationPolicy = mockk<TripAuthorizationPolicy>()
    private val tripRepository = mockk<TripRepository>()
    private val chatMessageRepository = mockk<ChatMessageRepository>()
    private val chatEventPublisher = mockk<ChatEventPublisher>()
    private lateinit var service: ChatMessageService

    @BeforeEach
    fun setUp() {
        service = ChatMessageService(
            currentActor,
            tripAuthorizationPolicy,
            tripRepository,
            chatMessageRepository,
            chatEventPublisher,
        )
    }

    @Test
    fun `send persists message and publishes event`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hash", nickname = "tribe")
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val tripMember = TripMember(member, trip, role = TripRole.MEMBER)
        trip.members.add(tripMember)

        every { currentActor.requireUserId() } returns 1L
        every { tripAuthorizationPolicy.isTripMember(5L) } returns true
        every { tripRepository.findTripWithMembersById(5L) } returns trip
        every { chatMessageRepository.save(any()) } answers { firstArg<ChatMessage>() }
        every { chatEventPublisher.publish(any()) } just runs

        val result = service.send(ChatMessageCommand.Send(5L, "hello"))

        assertEquals("hello", result.content)
        verify(exactly = 1) { chatEventPublisher.publish(any()) }
    }

    @Test
    fun `history returns first page using bounded limit`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hash", nickname = "tribe")
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val tripMember = TripMember(member, trip, role = TripRole.MEMBER)
        val message1 = ChatMessage(trip, tripMember, "first").apply { createdAt = LocalDateTime.of(2026, 4, 12, 12, 0) }
        val message2 = ChatMessage(trip, tripMember, "second").apply { createdAt = LocalDateTime.of(2026, 4, 12, 12, 1) }

        every { tripAuthorizationPolicy.isTripMember(5L) } returns true
        every { chatMessageRepository.findHistoryPage(5L, null, null, 21) } returns listOf(message2, message1)

        val history = service.history(ChatMessageQuery.History(5L, null, 20))

        assertEquals(2, history.content.size)
        assertEquals("second", history.content.first().content)
        assertEquals(false, history.hasNext)
        verify(exactly = 1) { chatMessageRepository.findHistoryPage(5L, null, null, 21) }
    }

    @Test
    fun `history decodes cursor and preserves pagination contract`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hash", nickname = "tribe")
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val tripMember = TripMember(member, trip, role = TripRole.MEMBER)
        val cursorCreatedAt = LocalDateTime.of(2026, 4, 12, 12, 2)
        val message1 = ChatMessage(trip, tripMember, "second").apply { createdAt = LocalDateTime.of(2026, 4, 12, 12, 1) }
        val message2 = ChatMessage(trip, tripMember, "first").apply { createdAt = LocalDateTime.of(2026, 4, 12, 12, 0) }
        val cursor = CursorCodec.encode(cursorCreatedAt, 42L)

        every { tripAuthorizationPolicy.isTripMember(5L) } returns true
        every { chatMessageRepository.findHistoryPage(5L, cursorCreatedAt, 42L, 2) } returns listOf(message1, message2)

        val history = service.history(ChatMessageQuery.History(5L, cursor, 1))

        assertEquals(1, history.content.size)
        assertEquals("second", history.content.first().content)
        assertEquals(true, history.hasNext)
        assertEquals(CursorCodec.encode(message1.createdAt, message1.id), history.nextCursor)
        verify(exactly = 1) { chatMessageRepository.findHistoryPage(5L, cursorCreatedAt, 42L, 2) }
    }
}
