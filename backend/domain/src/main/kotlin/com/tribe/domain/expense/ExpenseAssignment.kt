package com.tribe.domain.expense

import com.tribe.domain.trip.member.TripMember
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.math.BigDecimal

@Entity
class ExpenseAssignment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_item_id", nullable = false)
    val expenseItem: ExpenseItem,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_member_id", nullable = false)
    val tripMember: TripMember,
    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    val id: Long = 0L
}
