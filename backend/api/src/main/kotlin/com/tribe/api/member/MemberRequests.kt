package com.tribe.api.member

import com.tribe.application.member.MemberCommand
import jakarta.validation.constraints.NotBlank

/**
 * 회원 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object MemberRequests {
    data class UpdateNicknameRequest(
        @field:NotBlank(message = "닉네임은 비워둘 수 없습니다.")
        val nickname: String,
    ) {
        fun toCommand(): MemberCommand.UpdateNickname = MemberCommand.UpdateNickname(nickname)
    }
}
