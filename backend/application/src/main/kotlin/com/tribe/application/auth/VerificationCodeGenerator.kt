package com.tribe.application.auth

/**
 * 인증 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
interface VerificationCodeGenerator {
    fun generate(): String
}
