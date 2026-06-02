package com.tribe.domain.expense

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.math.BigDecimal

/**
 * 지출 도메인 상태 모델.
 *
 * 영속성 identity와 업무 규칙의 기준점.
 */
@Entity
class ExpenseItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    var expense: Expense,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false, precision = 19, scale = 2)
    var price: BigDecimal,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_item_id")
    val id: Long = 0L

    @OneToMany(mappedBy = "expenseItem", cascade = [CascadeType.ALL], orphanRemoval = true)
    val assignments: MutableList<ExpenseAssignment> = mutableListOf()

    fun replaceAssignments(nextAssignments: List<ExpenseAssignment>) {
        assignments.clear()
        assignments.addAll(nextAssignments)
    }
}
