package com.tribe.domain.chat

import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

/**
 * 채팅 도메인 상태 모델.
 *
 * 영속성 identity와 업무 규칙의 기준점.
 */
@Entity
class ChatMessage(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    val trip: Trip,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: TripMember,
    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    val id: Long = 0L

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
}
