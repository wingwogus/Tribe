package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseAssignment
import com.tribe.domain.expense.ExpenseCategory
import com.tribe.domain.expense.ExpenseItem
import com.tribe.domain.expense.ExpenseItemRepository
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.expense.ExpenseSplitType
import com.tribe.domain.expense.InputMethod
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class ExpenseService(
    private val expenseAuthorizationPolicy: ExpenseAuthorizationPolicy,
    private val expenseRepository: ExpenseRepository,
    private val expenseItemRepository: ExpenseItemRepository,
    private val itineraryItemRepository: ItineraryItemRepository,
    private val expenseReceiptAnalyzer: ExpenseReceiptAnalyzer,
    private val expenseReceiptStorage: ExpenseReceiptStorage,
) {

    fun createExpense(command: ExpenseCommand.Create): ExpenseResult.Detail {
        val actorMembership = expenseAuthorizationPolicy.requireMembership(command.tripId)
        val trip = actorMembership.trip
        val payer = resolveTripMember(trip, command.payerTripMemberId)
        val itineraryItem = resolveItineraryItem(command.tripId, command.itineraryItemId)
        val inputMethod = parseInputMethod(command.inputMethod)
        val receiptImageUrl = command.receiptImageBytes?.let { imageBytes ->
            expenseReceiptStorage.upload(
                imageBytes = imageBytes,
                folder = "receipts",
                mimeType = command.receiptImageContentType ?: "image/jpeg",
            )
        }

        val resolvedItems = when (inputMethod) {
            InputMethod.HANDWRITE -> resolveHandwriteItems(command)
            InputMethod.SCAN -> resolveScanItems(command)
        }
        val totalAmount = calculateTotalAmount(command.amount, resolvedItems)

        val expense = Expense(
            trip = trip,
            itineraryItem = itineraryItem,
            createdBy = actorMembership.member ?: throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP),
            payer = payer,
            title = command.title.trim(),
            amount = totalAmount,
            currencyCode = normalizeCurrencyCode(command.currencyCode),
            spentAt = command.spentAt,
            category = parseCategory(command.category),
            splitType = parseSplitType(command.splitType),
            inputMethod = inputMethod,
            note = command.note?.trim()?.takeIf { it.isNotEmpty() },
            receiptImageUrl = receiptImageUrl,
        )
        expense.replaceExpenseItems(
            resolvedItems.map { item ->
                ExpenseItem(
                    expense = expense,
                    name = item.itemName,
                    price = item.price,
                )
            },
        )

        return ExpenseResult.Detail.from(expenseRepository.save(expense))
    }

    @Transactional(readOnly = true)
    fun listExpenses(query: ExpenseQuery.ListByTrip): List<ExpenseResult.Summary> {
        expenseAuthorizationPolicy.requireMembership(query.tripId)
        return expenseRepository.findAllWithDetailsByTripId(query.tripId)
            .map(ExpenseResult.Summary::from)
    }

    @Transactional(readOnly = true)
    fun getExpenseDetail(query: ExpenseQuery.GetDetail): ExpenseResult.Detail {
        expenseAuthorizationPolicy.requireMembership(query.tripId)
        val expense = findExpense(query.expenseId)
        ensureTripMatch(expense, query.tripId)
        return ExpenseResult.Detail.from(expense)
    }

    fun updateExpense(command: ExpenseCommand.Update): ExpenseResult.Detail {
        val expense = findExpense(command.expenseId)
        ensureTripMatch(expense, command.tripId)
        expenseAuthorizationPolicy.requireModificationAccess(expense)

        val trip = expense.trip
        val nextItems = resolveUpdatedItems(expense, command)
        expense.update(
            title = command.title.trim(),
            amount = calculateItemizedTotalAmount(command.amount, nextItems),
            currencyCode = normalizeCurrencyCode(command.currencyCode),
            spentAt = command.spentAt,
            category = parseCategory(command.category),
            splitType = parseSplitType(command.splitType),
            payer = resolveTripMember(trip, command.payerTripMemberId),
            itineraryItem = resolveItineraryItem(command.tripId, command.itineraryItemId),
            inputMethod = parseInputMethod(command.inputMethod),
            note = command.note?.trim()?.takeIf { it.isNotEmpty() },
            receiptImageUrl = expense.receiptImageUrl,
        )
        expense.replaceExpenseItems(nextItems)
        return ExpenseResult.Detail.from(expense)
    }

    fun assignParticipants(command: ExpenseCommand.AssignParticipants): ExpenseResult.Detail {
        val expense = findExpense(command.expenseId)
        ensureTripMatch(expense, command.tripId)
        expenseAuthorizationPolicy.requireModificationAccess(expense)

        val trip = expense.trip
        val itemsById = expense.expenseItems.associateBy { it.id }

        command.items.forEach { itemAssignment ->
            val item = itemsById[itemAssignment.itemId]
                ?: throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("itemId" to itemAssignment.itemId))
            val participants = itemAssignment.participantIds.distinct().map {
                resolveTripMember(trip, it)
            }

            if (participants.isEmpty()) {
                item.replaceAssignments(emptyList())
            } else {
                val amounts = ExpenseCalculator.calculateFairShare(item.price, participants.size)
                item.replaceAssignments(
                    participants.zip(amounts).map { (participant, amount) ->
                        ExpenseAssignment(
                            expenseItem = item,
                            tripMember = participant,
                            amount = amount,
                        )
                    },
                )
            }
        }

        return ExpenseResult.Detail.from(expense)
    }

    fun clearAssignments(command: ExpenseCommand.ClearAssignments): ExpenseResult.Detail {
        val expense = findExpense(command.expenseId)
        ensureTripMatch(expense, command.tripId)
        expenseAuthorizationPolicy.requireModificationAccess(expense)

        val itemsById = expense.expenseItems.associateBy { it.id }
        command.itemIds.forEach { itemId ->
            val item = itemsById[itemId]
                ?: throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("itemId" to itemId))
            item.replaceAssignments(emptyList())
        }
        return ExpenseResult.Detail.from(expense)
    }

    fun deleteExpense(command: ExpenseCommand.Delete) {
        val expense = findExpense(command.expenseId)
        ensureTripMatch(expense, command.tripId)
        expenseAuthorizationPolicy.requireModificationAccess(expense)
        expenseRepository.delete(expense)
    }

    private fun findExpense(expenseId: Long): Expense =
        expenseRepository.findWithDetailsById(expenseId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

    private fun ensureTripMatch(expense: Expense, tripId: Long) {
        if (expense.trip.id != tripId) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    private fun resolveHandwriteItems(command: ExpenseCommand.Create): List<ExpenseCommand.Item> {
        if (command.items.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("items" to "required"))
        }
        if (command.amount == null) {
            throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("amount" to "required"))
        }
        return command.items
    }

    private fun resolveScanItems(command: ExpenseCommand.Create): List<ExpenseCommand.Item> {
        val imageBytes = command.receiptImageBytes
            ?: throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("image" to "required_for_scan"))
        val mimeType = command.receiptImageContentType ?: "image/jpeg"
        val analysis = expenseReceiptAnalyzer.analyze(imageBytes, mimeType)
        val normalizedItems = analysis.items.map {
            ExpenseCommand.Item(
                itemName = it.itemName,
                price = it.price,
            )
        }
        val rawItemsSum = normalizedItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.price) }
        val remainder = analysis.totalAmount.subtract(rawItemsSum)
        return when {
            remainder > BigDecimal.ZERO -> normalizedItems + ExpenseCommand.Item(
                itemName = "세금 / 팁 / 기타",
                price = remainder,
            )
            remainder < BigDecimal.ZERO -> normalizedItems + ExpenseCommand.Item(
                itemName = "할인",
                price = remainder,
            )
            else -> normalizedItems
        }
    }

    private fun calculateTotalAmount(expectedAmount: BigDecimal?, items: List<ExpenseCommand.Item>): BigDecimal {
        val sum = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.price) }
        if (expectedAmount == null) {
            return sum
        }
        if (expectedAmount.compareTo(sum) != 0) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                detail = mapOf("reason" to "expense_total_amount_mismatch", "expected" to expectedAmount, "actual" to sum),
            )
        }
        return expectedAmount
    }

    private fun calculateItemizedTotalAmount(expectedAmount: BigDecimal, items: List<ExpenseItem>): BigDecimal {
        val sum = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.price) }
        if (expectedAmount.compareTo(sum) != 0) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                detail = mapOf("reason" to "expense_total_amount_mismatch", "expected" to expectedAmount, "actual" to sum),
            )
        }
        return expectedAmount
    }

    private fun resolveUpdatedItems(expense: Expense, command: ExpenseCommand.Update): List<ExpenseItem> {
        val existingItems = expense.expenseItems.associateBy { it.id }
        val nextItems = mutableListOf<ExpenseItem>()

        command.items.forEach { itemCommand ->
            if (itemCommand.itemId == null) {
                nextItems.add(
                    ExpenseItem(
                        expense = expense,
                        name = itemCommand.itemName.trim(),
                        price = itemCommand.price,
                    ),
                )
                return@forEach
            }

            val existing = existingItems[itemCommand.itemId]
                ?: throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("itemId" to itemCommand.itemId))

            val priceChanged = existing.price.compareTo(itemCommand.price) != 0
            existing.name = itemCommand.itemName.trim()
            existing.price = itemCommand.price

            if (priceChanged && existing.assignments.isNotEmpty()) {
                val participants = existing.assignments.map { it.tripMember }
                val amounts = ExpenseCalculator.calculateFairShare(existing.price, participants.size)
                existing.replaceAssignments(
                    participants.zip(amounts).map { (participant, amount) ->
                        ExpenseAssignment(
                            expenseItem = existing,
                            tripMember = participant,
                            amount = amount,
                        )
                    },
                )
            }
            nextItems.add(existing)
        }

        return nextItems
    }

    private fun resolveTripMember(trip: Trip, tripMemberId: Long): TripMember =
        trip.members.firstOrNull {
            it.id == tripMemberId && it.role != TripRole.EXITED && it.role != TripRole.KICKED
        } ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

    private fun normalizeCurrencyCode(currencyCode: String): String {
        val normalized = currencyCode.trim().uppercase()
        if (normalized.length != 3) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        return normalized
    }

    private fun parseCategory(raw: String): ExpenseCategory =
        runCatching { ExpenseCategory.valueOf(raw.trim().uppercase()) }
            .getOrElse { throw BusinessException(ErrorCode.INVALID_INPUT) }

    private fun parseSplitType(raw: String): ExpenseSplitType =
        runCatching { ExpenseSplitType.valueOf(raw.trim().uppercase()) }
            .getOrElse { throw BusinessException(ErrorCode.INVALID_INPUT) }

    private fun parseInputMethod(raw: String): InputMethod =
        runCatching { InputMethod.valueOf(raw.trim().uppercase()) }
            .getOrElse { throw BusinessException(ErrorCode.INVALID_INPUT) }

    private fun resolveItineraryItem(tripId: Long, itineraryItemId: Long?): ItineraryItem? {
        if (itineraryItemId == null) return null
        val itineraryItem = itineraryItemRepository.findById(itineraryItemId)
            .orElseThrow { BusinessException(ErrorCode.ITEM_NOT_FOUND) }
        if (itineraryItem.trip.id != tripId) {
            throw BusinessException(ErrorCode.NO_BELONG_TRIP)
        }
        return itineraryItem
    }
}
