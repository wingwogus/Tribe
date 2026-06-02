package com.tribe.api.auth

import com.tribe.application.auth.AuthResult

/**
 * 인증 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
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
