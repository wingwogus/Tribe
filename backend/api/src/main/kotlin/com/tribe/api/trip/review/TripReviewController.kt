package com.tribe.api.trip.review

import com.tribe.api.common.ApiResponse
import com.tribe.application.trip.review.TripReviewCommand
import com.tribe.application.trip.review.TripReviewService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/reviews")
class TripReviewController(
    private val tripReviewService: TripReviewService,
) {
    @PostMapping
    fun createReview(
        @PathVariable tripId: Long,
        @RequestBody request: TripReviewRequests.CreateReviewRequest,
    ): ResponseEntity<ApiResponse<TripReviewResponses.ReviewDetailResponse>> {
        val result = tripReviewService.createReview(tripId, TripReviewCommand.Create(tripId, request.concept))
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(TripReviewResponses.ReviewDetailResponse.from(result)))
    }

    @GetMapping
    fun getAllReviews(
        @PathVariable tripId: Long,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ResponseEntity<ApiResponse<List<TripReviewResponses.SimpleReviewInfoResponse>>> {
        val result = tripReviewService.getAllReviews(tripId, pageable).content.map(TripReviewResponses.SimpleReviewInfoResponse::from)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @GetMapping("/{reviewId}")
    fun getReview(
        @PathVariable tripId: Long,
        @PathVariable reviewId: Long,
    ): ResponseEntity<ApiResponse<TripReviewResponses.ReviewDetailResponse>> {
        val result = tripReviewService.getReview(tripId, reviewId)
        return ResponseEntity.ok(ApiResponse.ok(TripReviewResponses.ReviewDetailResponse.from(result)))
    }
}
