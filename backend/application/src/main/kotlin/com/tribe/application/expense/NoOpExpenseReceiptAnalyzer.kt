package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * 지출 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
@Component
@Primary
class NoOpExpenseReceiptAnalyzer : ExpenseReceiptAnalyzer {
    override fun analyze(imageBytes: ByteArray, mimeType: String): ReceiptAnalysis {
        throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)
    }
}
