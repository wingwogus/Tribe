package com.tribe.domain.expense

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 지출 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface ExpenseRepository : JpaRepository<Expense, Long> {
    fun findAllByTripIdOrderBySpentAtDescIdDesc(tripId: Long): List<Expense>

    @Query(
        """
        select distinct e
        from Expense e
        join fetch e.trip
        join fetch e.createdBy
        join fetch e.payer payer
        left join fetch payer.member
        left join fetch e.itineraryItem itineraryItem
        left join fetch e.expenseItems item
        where e.id = :expenseId
        """
    )
    fun findWithDetailsById(@Param("expenseId") expenseId: Long): Expense?

    @Query(
        """
        select distinct e
        from Expense e
        join fetch e.trip
        join fetch e.createdBy
        join fetch e.payer payer
        left join fetch payer.member
        left join fetch e.itineraryItem itineraryItem
        left join fetch e.expenseItems item
        where e.trip.id = :tripId
        order by e.spentAt desc, e.id desc
        """
    )
    fun findAllWithDetailsByTripId(@Param("tripId") tripId: Long): List<Expense>
}
