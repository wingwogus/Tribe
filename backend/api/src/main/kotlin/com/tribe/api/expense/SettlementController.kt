package com.tribe.api.expense

import com.tribe.api.common.ApiResponse
import com.tribe.application.expense.SettlementService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 지출 HTTP 진입점.
 *
 * transport DTO와 application use case 연결 경계.
 */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/settlements")
class SettlementController(
    private val settlementService: SettlementService,
) {
    @GetMapping("/daily")
    fun getDailySettlement(
        @PathVariable tripId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ResponseEntity<ApiResponse<SettlementResponses.DailyResponse>> {
        return ResponseEntity.ok(ApiResponse.ok(SettlementResponses.from(settlementService.getDailySettlement(tripId, date))))
    }

    @GetMapping("/total")
    fun getTotalSettlement(
        @PathVariable tripId: Long,
    ): ResponseEntity<ApiResponse<SettlementResponses.TotalResponse>> {
        return ResponseEntity.ok(ApiResponse.ok(SettlementResponses.from(settlementService.getTotalSettlement(tripId))))
    }
}
