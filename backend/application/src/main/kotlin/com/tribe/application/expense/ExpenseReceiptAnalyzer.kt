package com.tribe.application.expense

import java.math.BigDecimal

/**
 * 지출 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
interface ExpenseReceiptAnalyzer {
    fun analyze(imageBytes: ByteArray, mimeType: String): ReceiptAnalysis
}

data class ReceiptAnalysis(
    val totalAmount: BigDecimal,
    val items: List<ReceiptItem>,
    val subtotal: BigDecimal? = null,
    val tax: BigDecimal? = null,
    val tip: BigDecimal? = null,
    val discount: BigDecimal? = null,
)

data class ReceiptItem(
    val itemName: String,
    val price: BigDecimal,
)
