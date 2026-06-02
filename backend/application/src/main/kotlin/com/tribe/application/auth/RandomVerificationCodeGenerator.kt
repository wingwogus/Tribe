package com.tribe.application.auth

import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * 인증 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
@Component
class RandomVerificationCodeGenerator : VerificationCodeGenerator {
    override fun generate(): String {
        return (1..6).joinToString("") {
            Random.nextInt(0, 10).toString()
        }
    }
}
