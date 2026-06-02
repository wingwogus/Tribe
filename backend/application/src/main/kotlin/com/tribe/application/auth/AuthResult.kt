package com.tribe.application.auth

/**
 * 인증 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object AuthResult {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val isFirstLogin: Boolean = false
    )
}
