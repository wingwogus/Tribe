package com.tribe.application.chat

import com.tribe.application.chat.event.ChatEventPublisher
import com.tribe.application.common.cursor.CursorCodec
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.core.TripAuthorizationPolicy
import com.tribe.domain.chat.ChatMessage
import com.tribe.domain.chat.ChatMessageRepository
import com.tribe.domain.trip.core.TripRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ChatMessageService(
    private val currentActor: CurrentActor,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
    private val tripRepository: TripRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatEventPublisher: ChatEventPublisher,
) {
    fun send(command: ChatMessageCommand.Send): ChatMessageResult.Message {
        val memberId = currentActor.requireUserId()
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val trip = tripRepository.findTripWithMembersById(command.tripId)
            ?: throw BusinessException(ErrorCode.TRIP_NOT_FOUND)
        val sender = trip.members.firstOrNull { it.member?.id == memberId }
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)

        val saved = chatMessageRepository.save(
            ChatMessage(
                trip = trip,
                sender = sender,
                content = command.content,
            )
        )
        val result = ChatMessageResult.Message.from(saved)
        chatEventPublisher.publish(ChatMessageResult.toEvent(result, command.tripId))
        return result
    }

    @Transactional(readOnly = true)
    fun history(query: ChatMessageQuery.History): ChatMessageResult.History {
        tripAuthorizationPolicy.isTripMember(query.tripId)
        val parsedCursor = CursorCodec.decode(query.cursor)
        val limit = query.pageSize.coerceIn(1, 100) + 1
        val messages = chatMessageRepository.findHistoryPage(
            tripId = query.tripId,
            cursorCreatedAt = parsedCursor?.createdAt,
            cursorId = parsedCursor?.id,
            org.springframework.data.domain.PageRequest.of(0, limit),
        ).toMutableList()

        val hasNext = messages.size > query.pageSize
        if (hasNext) {
            messages.removeAt(query.pageSize)
        }

        val content = messages.map(ChatMessageResult.Message::from)
        val nextCursor = if (hasNext && messages.isNotEmpty()) {
            val last = messages.last()
            CursorCodec.encode(last.createdAt, last.id)
        } else null

        return ChatMessageResult.History(content, nextCursor, hasNext)
    }
}
