package com.tribe.application.auth

/**
 * 인증 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object AuthCommand {
    data class SendVerificationCode(
        val email: String
    )

    data class VerifyEmailCode(
        val email: String,
        val code: String
    )

    data class SignUp(
        val email: String,
        val password: String,
        val nickname: String = "",
        val avatar: String? = null
    )

    data class Login(
        val email: String,
        val password: String
    )

    data class Reissue(
        val refreshToken: String
    )

    data class CheckNickname(
        val nickname: String
    )
}
