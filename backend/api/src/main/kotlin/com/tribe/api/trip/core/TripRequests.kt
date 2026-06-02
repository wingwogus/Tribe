package com.tribe.api.trip.core

import com.tribe.application.trip.core.TripCommand
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 여행 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object TripRequests {
    data class CreateRequest(
        @field:NotBlank(message = "여행 제목은 필수입니다.")
        val title: String,
        @field:NotNull(message = "여행 시작일은 필수입니다.")
        val startDate: LocalDate,
        @field:NotNull(message = "여행 종료일은 필수입니다.")
        val endDate: LocalDate,
        @field:NotBlank(message = "여행 국가는 필수입니다.")
        val country: String,
        val regionCode: String? = null,
    ) {
        @AssertTrue(message = "여행 시작일은 종료일보다 이전이거나 같아야 합니다.")
        fun isDatesValid(): Boolean = !startDate.isAfter(endDate)

        fun toCommand(): TripCommand.Create = TripCommand.Create(
            title = title,
            startDate = startDate,
            endDate = endDate,
            country = country,
            regionCode = regionCode,
        )
    }

    data class UpdateRequest(
        @field:NotBlank(message = "여행 제목은 필수입니다.")
        val title: String,
        @field:NotNull(message = "여행 시작일은 필수입니다.")
        val startDate: LocalDate,
        @field:NotNull(message = "여행 종료일은 필수입니다.")
        val endDate: LocalDate,
        @field:NotBlank(message = "여행 국가는 필수입니다.")
        val country: String,
        val regionCode: String? = null,
        val deleteOutOfRangeItems: Boolean = false,
    ) {
        @AssertTrue(message = "여행 시작일은 종료일보다 이전이거나 같아야 합니다.")
        fun isDatesValid(): Boolean = !startDate.isAfter(endDate)

        fun toCommand(tripId: Long): TripCommand.Update = TripCommand.Update(
            tripId = tripId,
            title = title,
            startDate = startDate,
            endDate = endDate,
            country = country,
            regionCode = regionCode,
            deleteOutOfRangeItems = deleteOutOfRangeItems,
        )
    }

    data class JoinRequest(
        @field:NotBlank(message = "초대 토큰은 필수입니다.")
        val token: String,
    ) {
        fun toCommand(): TripCommand.Join = TripCommand.Join(token)
    }

    data class ImportRequest(
        @field:NotNull(message = "포스트 아이디는 필수입니다.")
        val postId: Long,
        @field:NotBlank(message = "여행 제목은 필수입니다.")
        val title: String,
        @field:NotNull(message = "여행 시작일은 필수입니다.")
        val startDate: LocalDate,
        @field:NotNull(message = "여행 종료일은 필수입니다.")
        val endDate: LocalDate,
    ) {
        @AssertTrue(message = "여행 시작일은 종료일보다 이전이거나 같아야 합니다.")
        fun isDatesValid(): Boolean = !startDate.isAfter(endDate)

        fun toCommand(): TripCommand.Import = TripCommand.Import(
            postId = postId,
            title = title,
            startDate = startDate,
            endDate = endDate,
        )
    }

    data class AddGuestRequest(
        @field:NotBlank(message = "게스트 닉네임은 필수입니다.")
        val nickname: String,
    ) {
        fun toCommand(tripId: Long): TripCommand.AddGuest = TripCommand.AddGuest(
            tripId = tripId,
            nickname = nickname,
        )
    }

    data class AssignRoleRequest(
        @field:NotBlank(message = "역할은 필수입니다.")
        val role: String,
    ) {
        fun toCommand(tripId: Long, memberId: Long): TripCommand.AssignRole = TripCommand.AssignRole(
            tripId = tripId,
            memberId = memberId,
            role = role,
        )
    }
}
