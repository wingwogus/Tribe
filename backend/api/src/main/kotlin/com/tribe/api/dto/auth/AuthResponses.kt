package com.tribe.api.dto.auth

import com.tribe.application.auth.AuthResult

object AuthResponses {
    data class TokenResponse(
        val accessToken: String,
        val isFirstLogin: Boolean
    ) {
        companion object {
            fun from(result: AuthResult.TokenPair): TokenResponse {
                return TokenResponse(
                    accessToken = result.accessToken,
                    isFirstLogin = result.isFirstLogin
                )
            }
        }
    }
}
