package com.tribe.application.security

/**
 * 보안 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
interface CurrentActor {
    fun requireUserId(): Long
}
