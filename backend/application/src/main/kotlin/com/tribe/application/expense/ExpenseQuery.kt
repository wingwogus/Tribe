package com.tribe.application.expense

/**
 * 지출 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
object ExpenseQuery {
    data class ListByTrip(
        val tripId: Long,
    )

    data class GetDetail(
        val tripId: Long,
        val expenseId: Long,
    )
}
