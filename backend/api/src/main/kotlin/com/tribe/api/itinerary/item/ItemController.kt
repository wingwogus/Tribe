package com.tribe.api.itinerary.item

import com.tribe.api.common.ApiResponse
import com.tribe.application.itinerary.item.ItemCommand
import com.tribe.application.itinerary.item.ItemService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/items")
class ItemController(
    private val itemService: ItemService,
) {
    @PostMapping
    fun createItem(
        @PathVariable tripId: Long,
        @RequestBody request: ItemRequests.CreateRequest,
    ): ResponseEntity<ApiResponse<ItemResponses.ItemResponse>> {
        val result = itemService.createItem(
            ItemCommand.Create(
                tripId = tripId,
                visitDay = request.visitDay,
                placeId = request.placeId,
                title = request.title,
                time = request.time,
                memo = request.memo,
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(ItemResponses.ItemResponse.from(result)))
    }

    @GetMapping
    fun getAllItems(
        @PathVariable tripId: Long,
        @RequestParam(required = false) visitDay: Int?,
    ): ResponseEntity<ApiResponse<List<ItemResponses.ItemResponse>>> {
        val result = itemService.getAllItems(tripId, visitDay).map(ItemResponses.ItemResponse::from)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @GetMapping("/{itemId}")
    fun getItem(
        @PathVariable tripId: Long,
        @PathVariable itemId: Long,
    ): ResponseEntity<ApiResponse<ItemResponses.ItemResponse>> {
        val result = itemService.getItem(tripId, itemId)
        return ResponseEntity.ok(ApiResponse.ok(ItemResponses.ItemResponse.from(result)))
    }

    @PatchMapping("/{itemId}")
    fun updateItem(
        @PathVariable tripId: Long,
        @PathVariable itemId: Long,
        @RequestBody request: ItemRequests.UpdateRequest,
    ): ResponseEntity<ApiResponse<ItemResponses.ItemResponse>> {
        val result = itemService.updateItem(
            ItemCommand.Update(
                tripId = tripId,
                itemId = itemId,
                visitDay = request.visitDay,
                title = request.title,
                time = request.time,
                memo = request.memo,
            ),
        )
        return ResponseEntity.ok(ApiResponse.ok(ItemResponses.ItemResponse.from(result)))
    }

    @DeleteMapping("/{itemId}")
    fun deleteItem(
        @PathVariable tripId: Long,
        @PathVariable itemId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        itemService.deleteItem(tripId, itemId)
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }

    @PatchMapping("/order")
    fun updateItemOrder(
        @PathVariable tripId: Long,
        @RequestBody request: ItemRequests.OrderUpdateRequest,
    ): ResponseEntity<ApiResponse<List<ItemResponses.ItemResponse>>> {
        val result = itemService.updateItemOrder(
            ItemCommand.OrderUpdate(
                tripId = tripId,
                items = request.items.map {
                    ItemCommand.OrderItem(
                        itemId = it.itemId,
                        visitDay = it.visitDay,
                        itemOrder = it.itemOrder,
                    )
                },
            ),
        )
        return ResponseEntity.ok(ApiResponse.ok(result.map(ItemResponses.ItemResponse::from)))
    }

    @GetMapping("/directions")
    fun getAllDirections(
        @PathVariable tripId: Long,
        @RequestParam mode: String,
    ): ResponseEntity<ApiResponse<List<ItemResponses.RouteDetailsResponse>>> {
        val result = itemService.getAllDirectionsForTrip(tripId, mode)
            .map(ItemResponses.RouteDetailsResponse::from)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}
