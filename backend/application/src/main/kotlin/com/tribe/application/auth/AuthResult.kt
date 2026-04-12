package com.tribe.application.auth

object AuthResult {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val isFirstLogin: Boolean = false
    )
}
