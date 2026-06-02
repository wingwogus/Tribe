package com.tribe.application.expense

import com.tribe.domain.expense.Expense
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지출 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object ExpenseResult {
    data class ParticipantInfo(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val isGuest: Boolean,
    )

    data class ItemParticipantSummary(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val isGuest: Boolean,
        val amount: BigDecimal,
    )

    data class ItemSummary(
        val itemId: Long,
        val itemName: String,
        val price: BigDecimal,
    )

    data class ItemDetail(
        val itemId: Long,
        val itemName: String,
        val price: BigDecimal,
        val participants: List<ItemParticipantSummary>,
    )

    data class Summary(
        val expenseId: Long,
        val tripId: Long,
        val itineraryItemId: Long?,
        val title: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val payerTripMemberId: Long,
        val payerName: String,
        val itemCount: Int,
        val inputMethod: String,
        val receiptImageUrl: String?,
    ) {
        companion object {
            fun from(expense: Expense) = Summary(
                expenseId = expense.id,
                tripId = expense.trip.id,
                itineraryItemId = expense.itineraryItem?.id,
                title = expense.title,
                amount = expense.amount,
                currencyCode = expense.currencyCode,
                spentAt = expense.spentAt,
                category = expense.category.name,
                splitType = expense.splitType.name,
                payerTripMemberId = expense.payer.id,
                payerName = expense.payer.name,
                itemCount = expense.expenseItems.size,
                inputMethod = expense.inputMethod.name,
                receiptImageUrl = expense.receiptImageUrl,
            )
        }
    }

    data class Detail(
        val expenseId: Long,
        val tripId: Long,
        val itineraryItemId: Long?,
        val createdByMemberId: Long,
        val title: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val inputMethod: String,
        val payerTripMemberId: Long,
        val payerName: String,
        val note: String?,
        val receiptImageUrl: String?,
        val items: List<ItemDetail>,
    ) {
        companion object {
            fun from(expense: Expense) = Detail(
                expenseId = expense.id,
                tripId = expense.trip.id,
                itineraryItemId = expense.itineraryItem?.id,
                createdByMemberId = expense.createdBy.id,
                title = expense.title,
                amount = expense.amount,
                currencyCode = expense.currencyCode,
                spentAt = expense.spentAt,
                category = expense.category.name,
                splitType = expense.splitType.name,
                inputMethod = expense.inputMethod.name,
                payerTripMemberId = expense.payer.id,
                payerName = expense.payer.name,
                note = expense.note,
                receiptImageUrl = expense.receiptImageUrl,
                items = expense.expenseItems.map { item ->
                    ItemDetail(
                        itemId = item.id,
                        itemName = item.name,
                        price = item.price,
                        participants = item.assignments.map { assignment ->
                            ItemParticipantSummary(
                                tripMemberId = assignment.tripMember.id,
                                memberId = assignment.tripMember.member?.id,
                                nickname = assignment.tripMember.name,
                                isGuest = assignment.tripMember.isGuest,
                                amount = assignment.amount,
                            )
                        },
                    )
                },
            )
        }
    }
}
