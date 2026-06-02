package com.tribe.api.chat

import com.tribe.api.common.ApiResponse
import com.tribe.application.chat.ChatMessageCommand
import com.tribe.application.chat.ChatMessageQuery
import com.tribe.application.chat.ChatMessageService
import jakarta.validation.constraints.Max
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 채팅 HTTP 진입점.
 *
 * transport DTO와 application use case 연결 경계.
 */
@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/chat")
class ChatController(
    private val chatMessageService: ChatMessageService,
) {
    @PostMapping
    fun sendChat(
        @PathVariable tripId: Long,
        @RequestBody request: ChatMessageRequests.SendRequest,
    ): ResponseEntity<ApiResponse<ChatMessageResponses.MessageResponse>> {
        val message = chatMessageService.send(request.toCommand(tripId))
        return ResponseEntity.ok(ApiResponse.ok(ChatMessageResponses.MessageResponse.from(message)))
    }

    @GetMapping
    fun getChatHistory(
        @PathVariable tripId: Long,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Max(100) pageSize: Int,
    ): ResponseEntity<ApiResponse<ChatMessageResponses.HistoryResponse>> {
        val history = chatMessageService.history(ChatMessageQuery.History(tripId, cursor, pageSize))
        return ResponseEntity.ok(ApiResponse.ok(ChatMessageResponses.HistoryResponse.from(history)))
    }
}
