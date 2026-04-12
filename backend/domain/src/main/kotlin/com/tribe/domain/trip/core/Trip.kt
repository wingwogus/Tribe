package com.tribe.domain.trip.core

import com.tribe.domain.chat.ChatMessage
import com.tribe.domain.itinerary.category.Category
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.member.Member
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripRole
import com.tribe.domain.trip.review.TripReview
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

import java.time.LocalDate

@Entity
class Trip(
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false)
    var startDate: LocalDate,
    @Column(nullable = false)
    var endDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var country: Country,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    val id: Long = 0L

    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val members: MutableList<TripMember> = mutableListOf()

    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val categories: MutableList<Category> = mutableListOf()

    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val wishlistItems: MutableList<WishlistItem> = mutableListOf()

    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chatMessages: MutableList<ChatMessage> = mutableListOf()

    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reviews: MutableList<TripReview> = mutableListOf()

    fun update(title: String, startDate: LocalDate, endDate: LocalDate, country: Country) {
        this.title = title
        this.startDate = startDate
        this.endDate = endDate
        this.country = country
    }

    fun addMember(member: Member, role: TripRole): TripMember {
        val tripMember = TripMember(
            member = member,
            trip = this,
            role = role,
        )
        members.add(tripMember)
        member.tripMembers.add(tripMember)
        return tripMember
    }
}
