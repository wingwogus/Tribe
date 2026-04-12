package com.tribe.api.trip.core

import com.tribe.api.common.ApiResponse
import com.tribe.application.trip.core.TripCommand
import com.tribe.application.trip.core.TripService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips")
class TripController(
    private val tripService: TripService,
) {
    @PostMapping
    fun createTrip(
        @Valid @RequestBody request: TripRequests.CreateRequest,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.createTrip(
            TripCommand.Create(request.title, request.startDate, request.endDate, request.country),
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @GetMapping
    fun getAllTrips(
        @PageableDefault(size = 10, sort = ["startDate"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ResponseEntity<ApiResponse<List<TripResponses.SimpleTripResponse>>> {
        val result = tripService.getAllTrips(pageable).content.map(TripResponses.SimpleTripResponse::from)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @GetMapping("/{tripId}")
    fun getTripDetails(@PathVariable tripId: Long): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.getTripDetails(tripId)
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @PatchMapping("/{tripId}")
    fun updateTrip(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: TripRequests.UpdateRequest,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.updateTrip(
            TripCommand.Update(tripId, request.title, request.startDate, request.endDate, request.country),
        )
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @DeleteMapping("/{tripId}")
    fun deleteTrip(@PathVariable tripId: Long): ResponseEntity<ApiResponse<Unit>> {
        tripService.deleteTrip(tripId)
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }

    @PostMapping("/{tripId}/invite")
    fun createInvitation(@PathVariable tripId: Long): ResponseEntity<ApiResponse<TripResponses.InvitationResponse>> {
        val result = tripService.createInvitation(tripId)
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.InvitationResponse.from(result)))
    }

    @PostMapping("/join")
    fun joinTrip(
        @Valid @RequestBody request: TripRequests.JoinRequest,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.joinTrip(TripCommand.Join(request.token))
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @PostMapping("/import")
    fun importTrip(
        @Valid @RequestBody request: TripRequests.ImportRequest,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.importTrip(
            TripCommand.Import(request.postId, request.title, request.startDate, request.endDate),
        )
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @PostMapping("/{tripId}/guests")
    fun addGuest(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: TripRequests.AddGuestRequest,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.addGuest(TripCommand.AddGuest(tripId, request.nickname))
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @DeleteMapping("/{tripId}/guests/{tripMemberId}")
    fun deleteGuest(
        @PathVariable tripId: Long,
        @PathVariable tripMemberId: Long,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.deleteGuest(TripCommand.DeleteGuest(tripId, tripMemberId))
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @DeleteMapping("/{tripId}/members/me")
    fun leaveTrip(@PathVariable tripId: Long): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.leaveTrip(TripCommand.Leave(tripId))
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @DeleteMapping("/{tripId}/members/{memberId}")
    fun kickMember(
        @PathVariable tripId: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.kickMember(TripCommand.KickMember(tripId, memberId))
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }

    @PatchMapping("/{tripId}/members/{memberId}/role")
    fun assignRole(
        @PathVariable tripId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: TripRequests.AssignRoleRequest,
    ): ResponseEntity<ApiResponse<TripResponses.TripDetailResponse>> {
        val result = tripService.assignRole(TripCommand.AssignRole(tripId, memberId, request.role))
        return ResponseEntity.ok(ApiResponse.ok(TripResponses.TripDetailResponse.from(result)))
    }
}
