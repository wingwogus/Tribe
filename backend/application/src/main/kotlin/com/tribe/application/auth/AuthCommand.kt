package com.tribe.application.auth

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
