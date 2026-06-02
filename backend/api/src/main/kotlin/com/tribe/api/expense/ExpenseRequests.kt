package com.tribe.api.expense

import com.tribe.application.expense.ExpenseCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지출 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object ExpenseRequests {
    data class ItemRequest(
        val itemId: Long? = null,
        @field:NotBlank(message = "항목 이름은 필수입니다.")
        val itemName: String,
        @field:NotNull(message = "항목 가격은 필수입니다.")
        @field:DecimalMin(value = "0.0", inclusive = true, message = "항목 가격은 0 이상이어야 합니다.")
        val price: BigDecimal,
    ) {
        fun toCommand(): ExpenseCommand.Item = ExpenseCommand.Item(
            itemId = itemId,
            itemName = itemName,
            price = price,
        )
    }

    data class ItemAssignmentRequest(
        @field:NotNull(message = "항목 ID는 필수입니다.")
        val itemId: Long,
        @field:NotEmpty(message = "참여자 목록은 비어있을 수 없습니다.")
        val participantIds: List<Long>,
    ) {
        fun toCommand(): ExpenseCommand.ItemAssignment = ExpenseCommand.ItemAssignment(
            itemId = itemId,
            participantIds = participantIds,
        )
    }

    data class CreateRequest(
        @field:NotBlank(message = "지출 제목은 필수입니다.")
        val title: String,
        @field:DecimalMin(value = "0.0", inclusive = false, message = "지출 금액은 0보다 커야 합니다.")
        val amount: BigDecimal? = null,
        @field:NotBlank(message = "통화 코드는 필수입니다.")
        val currencyCode: String,
        @field:NotNull(message = "지출 일자는 필수입니다.")
        val spentAt: LocalDate,
        @field:NotBlank(message = "지출 카테고리는 필수입니다.")
        val category: String,
        @field:NotBlank(message = "정산 방식은 필수입니다.")
        val splitType: String,
        @field:NotNull(message = "결제자 ID는 필수입니다.")
        val payerTripMemberId: Long,
        val itineraryItemId: Long? = null,
        @field:NotBlank(message = "입력 방식은 필수입니다.")
        val inputMethod: String,
        val note: String? = null,
        @field:Valid
        val items: List<ItemRequest> = emptyList(),
    ) {
        fun toCommand(tripId: Long, imageFile: MultipartFile?): ExpenseCommand.Create = ExpenseCommand.Create(
            tripId = tripId,
            title = title,
            amount = amount,
            currencyCode = currencyCode,
            spentAt = spentAt,
            category = category,
            splitType = splitType,
            payerTripMemberId = payerTripMemberId,
            itineraryItemId = itineraryItemId,
            inputMethod = inputMethod,
            note = note,
            items = items.map(ItemRequest::toCommand),
            receiptImageBytes = imageFile?.bytes,
            receiptImageContentType = imageFile?.contentType,
        )
    }

    data class UpdateRequest(
        @field:NotBlank(message = "지출 제목은 필수입니다.")
        val title: String,
        @field:DecimalMin(value = "0.0", inclusive = false, message = "지출 금액은 0보다 커야 합니다.")
        val amount: BigDecimal,
        @field:NotBlank(message = "통화 코드는 필수입니다.")
        val currencyCode: String,
        @field:NotNull(message = "지출 일자는 필수입니다.")
        val spentAt: LocalDate,
        @field:NotBlank(message = "지출 카테고리는 필수입니다.")
        val category: String,
        @field:NotBlank(message = "정산 방식은 필수입니다.")
        val splitType: String,
        @field:NotNull(message = "결제자 ID는 필수입니다.")
        val payerTripMemberId: Long,
        val itineraryItemId: Long? = null,
        @field:NotBlank(message = "입력 방식은 필수입니다.")
        val inputMethod: String,
        val note: String? = null,
        @field:Valid
        val items: List<ItemRequest> = emptyList(),
    ) {
        fun toCommand(tripId: Long, expenseId: Long): ExpenseCommand.Update = ExpenseCommand.Update(
            tripId = tripId,
            expenseId = expenseId,
            title = title,
            amount = amount,
            currencyCode = currencyCode,
            spentAt = spentAt,
            category = category,
            splitType = splitType,
            payerTripMemberId = payerTripMemberId,
            itineraryItemId = itineraryItemId,
            inputMethod = inputMethod,
            note = note,
            items = items.map(ItemRequest::toCommand),
        )
    }

    data class AssignParticipantsRequest(
        @field:Valid
        @field:NotEmpty(message = "배정 항목은 비어있을 수 없습니다.")
        val items: List<ItemAssignmentRequest>,
    ) {
        fun toCommand(tripId: Long, expenseId: Long): ExpenseCommand.AssignParticipants = ExpenseCommand.AssignParticipants(
            tripId = tripId,
            expenseId = expenseId,
            items = items.map(ItemAssignmentRequest::toCommand),
        )
    }

    data class ClearAssignmentsRequest(
        @field:NotEmpty(message = "삭제할 항목 ID는 비어있을 수 없습니다.")
        val itemIds: List<Long>,
    ) {
        fun toCommand(tripId: Long, expenseId: Long): ExpenseCommand.ClearAssignments = ExpenseCommand.ClearAssignments(
            tripId = tripId,
            expenseId = expenseId,
            itemIds = itemIds,
        )
    }
}
