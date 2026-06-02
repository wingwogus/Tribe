package com.tribe.api.auth

import com.tribe.application.auth.AuthCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 인증 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object AuthRequests {
    data class SendVerificationCodeRequest(
        @field:NotBlank(message = "이메일을 입력해주세요")
        @field:Email(message = "이메일 형식이 올바르지 않습니다")
        val email: String
    ) {
        fun toCommand(): AuthCommand.SendVerificationCode = AuthCommand.SendVerificationCode(email)
    }

    data class VerifyEmailCodeRequest(
        @field:NotBlank(message = "이메일을 입력해주세요")
        @field:Email(message = "이메일 형식이 올바르지 않습니다")
        val email: String,
        @field:NotBlank(message = "인증번호를 입력해주세요")
        @field:Size(min = 6, max = 6, message = "인증번호는 6자리여야 합니다")
        val code: String
    ) {
        fun toCommand(): AuthCommand.VerifyEmailCode = AuthCommand.VerifyEmailCode(
            email = email,
            code = code,
        )
    }

    data class SignUpRequest(
        @field:NotBlank(message = "이메일을 입력해주세요")
        @field:Email(message = "이메일 형식이 올바르지 않습니다")
        val email: String,
        @field:NotBlank(message = "비밀번호를 입력해주세요")
        @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        val password: String,
        @field:NotBlank(message = "닉네임을 입력해주세요")
        @field:Size(max = 20, message = "닉네임은 20자 이하여야 합니다")
        val nickname: String,
        val avatar: String? = null
    ) {
        fun toCommand(): AuthCommand.SignUp = AuthCommand.SignUp(
            email = email,
            password = password,
            nickname = nickname,
            avatar = avatar,
        )
    }

    data class LoginRequest(
        @field:NotBlank(message = "이메일을 입력해주세요")
        @field:Email(message = "이메일 형식이 올바르지 않습니다")
        val email: String,
        @field:NotBlank(message = "비밀번호를 입력해주세요")
        val password: String
    ) {
        fun toCommand(): AuthCommand.Login = AuthCommand.Login(
            email = email,
            password = password,
        )
    }

    data class CheckNicknameRequest(
        @field:NotBlank(message = "닉네임을 입력해주세요")
        @field:Size(max = 20, message = "닉네임은 20자 이하여야 합니다")
        val nickname: String
    ) {
        fun toCommand(): AuthCommand.CheckNickname = AuthCommand.CheckNickname(nickname)
    }
}
