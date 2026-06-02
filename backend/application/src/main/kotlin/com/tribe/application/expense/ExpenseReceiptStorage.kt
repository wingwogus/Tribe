package com.tribe.application.expense

/**
 * 지출 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
interface ExpenseReceiptStorage {
    fun upload(imageBytes: ByteArray, folder: String, mimeType: String): String
}
