package com.tribe.domain.trip.member

import com.tribe.domain.chat.ChatMessage
import com.tribe.domain.member.Member
import com.tribe.domain.trip.core.Trip
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class TripMember(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    val trip: Trip,
    @Column
    var guestNickname: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: TripRole,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_member_id")
    val id: Long = 0L

    @jakarta.persistence.OneToMany(mappedBy = "sender", cascade = [jakarta.persistence.CascadeType.ALL], orphanRemoval = true)
    val chatMessages: MutableList<ChatMessage> = mutableListOf()

    val name: String
        get() = member?.nickname ?: guestNickname ?: "unknown"

    val isGuest: Boolean
        get() = role == TripRole.GUEST
}
