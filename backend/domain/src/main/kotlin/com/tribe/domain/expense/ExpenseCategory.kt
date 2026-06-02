package com.tribe.domain.expense

/**
 * 지출 도메인 enum 경계.
 *
 * 업무 상태/분류 값을 코드 계약으로 고정.
 */
enum class ExpenseCategory {
    TRANSPORT,
    ACCOMMODATION,
    FOOD,
    ACTIVITY,
    SHOPPING,
    OTHER,
}
