package com.tribe.application.exception.business

import com.tribe.application.exception.ApplicationException
import com.tribe.application.exception.ErrorCode

open class BusinessException(
    val errorCode: ErrorCode,
    val detail: Any? = null,
    val customMessage: String? = null,
    message: String = customMessage ?: errorCode.messageKey
) : ApplicationException(message)
