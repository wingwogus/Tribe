package com.tribe.api.expense

import com.tribe.api.common.ApiResponse
import com.tribe.application.expense.ExpenseCommand
import com.tribe.application.expense.ExpenseQuery
import com.tribe.application.expense.ExpenseService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 지출 HTTP 진입점.
 *
 * transport DTO와 application use case 연결 경계.
 */
@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses")
class ExpenseController(
    private val expenseService: ExpenseService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createExpense(
        @PathVariable tripId: Long,
        @Valid @RequestPart("request") request: ExpenseRequests.CreateRequest,
        @RequestPart(value = "image", required = false) imageFile: MultipartFile?,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = expenseService.createExpense(request.toCommand(tripId, imageFile))
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @GetMapping
    fun listExpenses(
        @PathVariable tripId: Long,
    ): ResponseEntity<ApiResponse<List<ExpenseResponses.ExpenseSummaryResponse>>> {
        val result = expenseService.listExpenses(ExpenseQuery.ListByTrip(tripId))
        return ResponseEntity.ok(ApiResponse.ok(result.map(ExpenseResponses.ExpenseSummaryResponse::from)))
    }

    @GetMapping("/{expenseId}")
    fun getExpenseDetail(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = expenseService.getExpenseDetail(ExpenseQuery.GetDetail(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @PatchMapping("/{expenseId}")
    fun updateExpense(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
        @Valid @RequestBody request: ExpenseRequests.UpdateRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = expenseService.updateExpense(request.toCommand(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @PostMapping("/{expenseId}/assignments")
    fun assignParticipants(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
        @Valid @RequestBody request: ExpenseRequests.AssignParticipantsRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = expenseService.assignParticipants(request.toCommand(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @PostMapping("/{expenseId}/assignments:clear")
    fun clearAssignments(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
        @Valid @RequestBody request: ExpenseRequests.ClearAssignmentsRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = expenseService.clearAssignments(request.toCommand(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @DeleteMapping("/{expenseId}")
    fun deleteExpense(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        expenseService.deleteExpense(ExpenseCommand.Delete(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }
}
