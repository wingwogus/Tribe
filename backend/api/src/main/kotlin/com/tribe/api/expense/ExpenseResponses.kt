package com.tribe.api.expense

import com.tribe.application.expense.ExpenseResult
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지출 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object ExpenseResponses {
    data class ParticipantInfoResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val isGuest: Boolean,
    ) {
        companion object {
            fun from(result: ExpenseResult.ParticipantInfo) = ParticipantInfoResponse(
                tripMemberId = result.tripMemberId,
                memberId = result.memberId,
                nickname = result.nickname,
                isGuest = result.isGuest,
            )
        }
    }

    data class ItemParticipantResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val isGuest: Boolean,
        val amount: BigDecimal,
    ) {
        companion object {
            fun from(result: ExpenseResult.ItemParticipantSummary) = ItemParticipantResponse(
                tripMemberId = result.tripMemberId,
                memberId = result.memberId,
                nickname = result.nickname,
                isGuest = result.isGuest,
                amount = result.amount,
            )
        }
    }

    data class ItemResponse(
        val itemId: Long,
        val itemName: String,
        val price: BigDecimal,
    ) {
        companion object {
            fun from(result: ExpenseResult.ItemSummary) = ItemResponse(
                itemId = result.itemId,
                itemName = result.itemName,
                price = result.price,
            )
        }
    }

    data class ItemDetailResponse(
        val itemId: Long,
        val itemName: String,
        val price: BigDecimal,
        val participants: List<ItemParticipantResponse>,
    ) {
        companion object {
            fun from(result: ExpenseResult.ItemDetail) = ItemDetailResponse(
                itemId = result.itemId,
                itemName = result.itemName,
                price = result.price,
                participants = result.participants.map(ItemParticipantResponse::from),
            )
        }
    }

    data class ExpenseSummaryResponse(
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
            fun from(result: ExpenseResult.Summary) = ExpenseSummaryResponse(
                expenseId = result.expenseId,
                tripId = result.tripId,
                itineraryItemId = result.itineraryItemId,
                title = result.title,
                amount = result.amount,
                currencyCode = result.currencyCode,
                spentAt = result.spentAt,
                category = result.category,
                splitType = result.splitType,
                payerTripMemberId = result.payerTripMemberId,
                payerName = result.payerName,
                itemCount = result.itemCount,
                inputMethod = result.inputMethod,
                receiptImageUrl = result.receiptImageUrl,
            )
        }
    }

    data class ExpenseDetailResponse(
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
        val items: List<ItemDetailResponse>,
    ) {
        companion object {
            fun from(result: ExpenseResult.Detail) = ExpenseDetailResponse(
                expenseId = result.expenseId,
                tripId = result.tripId,
                itineraryItemId = result.itineraryItemId,
                createdByMemberId = result.createdByMemberId,
                title = result.title,
                amount = result.amount,
                currencyCode = result.currencyCode,
                spentAt = result.spentAt,
                category = result.category,
                splitType = result.splitType,
                inputMethod = result.inputMethod,
                payerTripMemberId = result.payerTripMemberId,
                payerName = result.payerName,
                note = result.note,
                receiptImageUrl = result.receiptImageUrl,
                items = result.items.map(ItemDetailResponse::from),
            )
        }
    }
}
