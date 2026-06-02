package com.tribe.application.auth

/**
 * 인증 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
interface EmailSender {
    fun sendVerificationCode(email: String, code: String)
}
