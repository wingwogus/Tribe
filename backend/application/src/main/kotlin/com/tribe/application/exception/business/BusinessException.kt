package com.tribe.application.exception.business

import com.tribe.application.exception.ApplicationException
import com.tribe.application.exception.ErrorCode

/**
 * 업무 규칙 예외 경계.
 *
 * ErrorCode와 선택 detail을 API 오류 envelope로 전달하는 계약.
 */
open class BusinessException(
    val errorCode: ErrorCode,
    val detail: Any? = null,
    val customMessage: String? = null,
    message: String = customMessage ?: errorCode.messageKey
) : ApplicationException(message)
