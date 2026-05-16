package com.tribe.api.itinerary.wishlist

import com.tribe.api.common.ApiResponse
import com.tribe.application.itinerary.wishlist.WishlistCommand
import com.tribe.application.itinerary.wishlist.WishlistService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/wishlists")
class WishlistController(
    private val wishlistService: WishlistService,
) {
    @PostMapping
    fun addWishlistItem(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: WishlistRequests.WishlistAddRequest,
    ): ResponseEntity<ApiResponse<WishlistResponses.WishlistItemResponse>> {
        val result = wishlistService.addWishList(request.toCommand(tripId))
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(WishlistResponses.WishlistItemResponse.from(result)))
    }

    @PostMapping("/from-member-wishlist")
    fun addWishlistItemFromMemberWishlist(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: WishlistRequests.WishlistAddFromMemberWishlistRequest,
    ): ResponseEntity<ApiResponse<WishlistResponses.WishlistItemResponse>> {
        val result = wishlistService.addWishListFromMemberWishlist(request.toCommand(tripId))
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(WishlistResponses.WishlistItemResponse.from(result)))
    }

    @GetMapping
    fun getWishlistItems(
        @PathVariable tripId: Long,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) wishlistSort: String?,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<WishlistResponses.WishlistSearchResponse>> {
        val effectiveSort = wishlistSort ?: sort
        val result = if (query.isNullOrBlank()) {
            wishlistService.getWishList(tripId, pageable, effectiveSort)
        } else {
            wishlistService.searchWishList(tripId, query, pageable, effectiveSort)
        }
        return ResponseEntity.ok(ApiResponse.ok(WishlistResponses.WishlistSearchResponse.from(result)))
    }

    @PostMapping("/{wishlistItemId}/likes")
    fun likeWishlistItem(
        @PathVariable tripId: Long,
        @PathVariable wishlistItemId: Long,
    ): ResponseEntity<ApiResponse<WishlistResponses.WishlistLikeResponse>> {
        val result = wishlistService.likeWishlistItem(WishlistCommand.Like(tripId, wishlistItemId))
        return ResponseEntity.ok(ApiResponse.ok(WishlistResponses.WishlistLikeResponse.from(result)))
    }

    @DeleteMapping("/{wishlistItemId}/likes")
    fun unlikeWishlistItem(
        @PathVariable tripId: Long,
        @PathVariable wishlistItemId: Long,
    ): ResponseEntity<ApiResponse<WishlistResponses.WishlistLikeResponse>> {
        val result = wishlistService.unlikeWishlistItem(WishlistCommand.Like(tripId, wishlistItemId))
        return ResponseEntity.ok(ApiResponse.ok(WishlistResponses.WishlistLikeResponse.from(result)))
    }

    @DeleteMapping
    fun deleteWishlistItems(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: WishlistRequests.WishlistDeleteRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        wishlistService.deleteWishlistItems(request.toCommand(tripId))
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }
}
