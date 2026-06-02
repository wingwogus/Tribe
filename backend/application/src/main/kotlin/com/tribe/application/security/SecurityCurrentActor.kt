package com.tribe.application.security

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * 보안 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
@Component
class SecurityCurrentActor : CurrentActor {
    override fun requireUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        return principal.toLongOrNull() ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
    }
}
