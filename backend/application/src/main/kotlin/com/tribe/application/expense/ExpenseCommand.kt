package com.tribe.application.expense

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지출 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object ExpenseCommand {
    data class Item(
        val itemId: Long? = null,
        val itemName: String,
        val price: BigDecimal,
    )

    data class ItemAssignment(
        val itemId: Long,
        val participantIds: List<Long>,
    )

    data class Create(
        val tripId: Long,
        val title: String,
        val amount: BigDecimal?,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val payerTripMemberId: Long,
        val itineraryItemId: Long?,
        val inputMethod: String,
        val note: String? = null,
        val items: List<Item> = emptyList(),
        val receiptImageBytes: ByteArray? = null,
        val receiptImageContentType: String? = null,
    )

    data class Update(
        val tripId: Long,
        val expenseId: Long,
        val title: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val payerTripMemberId: Long,
        val itineraryItemId: Long?,
        val inputMethod: String,
        val note: String? = null,
        val items: List<Item> = emptyList(),
    )

    data class AssignParticipants(
        val tripId: Long,
        val expenseId: Long,
        val items: List<ItemAssignment>,
    )

    data class ClearAssignments(
        val tripId: Long,
        val expenseId: Long,
        val itemIds: List<Long>,
    )

    data class Delete(
        val tripId: Long,
        val expenseId: Long,
    )
}
