package com.tribe.api.itinerary.wishlist

import com.tribe.api.common.ApiResponse
import com.tribe.application.itinerary.wishlist.MemberWishlistService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members/me/wishlists")
class MemberWishlistController(
    private val memberWishlistService: MemberWishlistService,
) {
    @PostMapping
    fun addWishlistItem(
        @Valid @RequestBody request: MemberWishlistRequests.MemberWishlistAddRequest,
    ): ResponseEntity<ApiResponse<MemberWishlistResponses.MemberWishlistItemResponse>> {
        val result = memberWishlistService.addWishlistItem(request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(MemberWishlistResponses.MemberWishlistItemResponse.from(result)))
    }

    @GetMapping
    fun getWishlistItems(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) wishlistSort: String?,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<MemberWishlistResponses.MemberWishlistSearchResponse>> {
        val effectiveSort = wishlistSort ?: sort
        val result = if (query.isNullOrBlank()) {
            memberWishlistService.getWishlist(pageable, effectiveSort)
        } else {
            memberWishlistService.searchWishlist(query, pageable, effectiveSort)
        }
        return ResponseEntity.ok(ApiResponse.ok(MemberWishlistResponses.MemberWishlistSearchResponse.from(result)))
    }

    @DeleteMapping
    fun deleteWishlistItems(
        @Valid @RequestBody request: MemberWishlistRequests.MemberWishlistDeleteRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        memberWishlistService.deleteWishlistItems(request.toCommand())
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }
}
