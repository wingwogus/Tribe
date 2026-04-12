package com.tribe.domain.expense

import com.tribe.domain.member.Member
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class Expense(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    val trip: Trip,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_item_id")
    var itineraryItem: ItineraryItem? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    val createdBy: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_trip_member_id", nullable = false)
    var payer: TripMember,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,
    @Column(nullable = false, length = 3)
    var currencyCode: String,
    @Column(nullable = false)
    var spentAt: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: ExpenseCategory,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var splitType: ExpenseSplitType,
    @Enumerated(EnumType.STRING)
    @Column(name = "input_method", nullable = false)
    var inputMethod: InputMethod = InputMethod.HANDWRITE,
    @Column(length = 1000)
    var note: String? = null,
    @Column(name = "receipt_image_url")
    var receiptImageUrl: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    val id: Long = 0L

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], orphanRemoval = true)
    val participants: MutableList<ExpenseParticipant> = mutableListOf()

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], orphanRemoval = true)
    val expenseItems: MutableList<ExpenseItem> = mutableListOf()

    fun update(
        title: String,
        amount: BigDecimal,
        currencyCode: String,
        spentAt: LocalDate,
        category: ExpenseCategory,
        splitType: ExpenseSplitType,
        payer: TripMember,
        note: String?,
        itineraryItem: ItineraryItem?,
        inputMethod: InputMethod,
        receiptImageUrl: String?,
    ) {
        this.title = title
        this.amount = amount
        this.currencyCode = currencyCode
        this.spentAt = spentAt
        this.category = category
        this.splitType = splitType
        this.payer = payer
        this.note = note
        this.itineraryItem = itineraryItem
        this.inputMethod = inputMethod
        this.receiptImageUrl = receiptImageUrl
    }

    fun replaceParticipants(nextParticipants: List<ExpenseParticipant>) {
        participants.clear()
        participants.addAll(nextParticipants)
    }

    fun addExpenseItem(expenseItem: ExpenseItem) {
        expenseItems.add(expenseItem)
        expenseItem.expense = this
    }

    fun replaceExpenseItems(nextItems: List<ExpenseItem>) {
        expenseItems.clear()
        expenseItems.addAll(nextItems)
    }
}
