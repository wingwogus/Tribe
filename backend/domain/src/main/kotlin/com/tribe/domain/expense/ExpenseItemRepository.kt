package com.tribe.domain.expense

import org.springframework.data.jpa.repository.JpaRepository

/**
 * 지출 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface ExpenseItemRepository : JpaRepository<ExpenseItem, Long>
