package com.tribe.api.trip.core

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

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
    ) {
        @AssertTrue(message = "여행 시작일은 종료일보다 이전이거나 같아야 합니다.")
        fun isDatesValid(): Boolean = !startDate.isAfter(endDate)
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
    ) {
        @AssertTrue(message = "여행 시작일은 종료일보다 이전이거나 같아야 합니다.")
        fun isDatesValid(): Boolean = !startDate.isAfter(endDate)
    }

    data class JoinRequest(
        @field:NotBlank(message = "초대 토큰은 필수입니다.")
        val token: String,
    )

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
    }

    data class AddGuestRequest(
        @field:NotBlank(message = "게스트 닉네임은 필수입니다.")
        val nickname: String,
    )

    data class AssignRoleRequest(
        @field:NotBlank(message = "역할은 필수입니다.")
        val role: String,
    )
}
