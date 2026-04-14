package com.tribe.application.bootstrap

import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseAssignment
import com.tribe.domain.expense.ExpenseCategory
import com.tribe.domain.expense.ExpenseItem
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.expense.ExpenseSplitType
import com.tribe.domain.expense.InputMethod
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.itinerary.wishlist.WishlistItem
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Profile("local", "dev")
@ConditionalOnProperty(name = ["tribe.seed.enabled"], havingValue = "true")
class LocalSeedService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tripRepository: TripRepository,
    private val tripMemberRepository: TripMemberRepository,
    private val placeRepository: PlaceRepository,
    private val wishlistItemRepository: WishlistItemRepository,
    private val expenseRepository: ExpenseRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (memberRepository.findByEmail("seed.owner@tribe.local") != null) {
            logger.info("Local seed skipped; seed owner already exists")
            return
        }

        val owner = memberRepository.save(
            Member(
                email = "seed.owner@tribe.local",
                passwordHash = passwordEncoder.encode("password"),
                nickname = "seed-owner",
            ),
        )
        val member = memberRepository.save(
            Member(
                email = "seed.member@tribe.local",
                passwordHash = passwordEncoder.encode("password"),
                nickname = "seed-member",
            ),
        )

        val trip = Trip(
            title = "Seed Tokyo Trip",
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(2),
            country = Country.JAPAN,
        )
        val ownerMembership = trip.addMember(owner, TripRole.OWNER)
        val memberMembership = trip.addMember(member, TripRole.MEMBER)

        val place = placeRepository.save(
            Place(
                externalPlaceId = "seed-place-1",
                name = "도톤보리",
                address = "오사카 도톤보리",
                latitude = BigDecimal("34.6687"),
                longitude = BigDecimal("135.5013"),
            ),
        )
        val breakfastItem = ItineraryItem(
            trip = trip,
            visitDay = 1,
            place = null,
            title = "호텔 조식",
            time = LocalDateTime.now().withHour(8).withMinute(0),
            order = 1,
            memo = "뷔페",
        )
        val dinnerItem = ItineraryItem(
            trip = trip,
            visitDay = 1,
            place = place,
            title = null,
            time = LocalDateTime.now().withHour(19).withMinute(0),
            order = 1,
            memo = "라멘",
        )

        trip.itineraryItems.add(breakfastItem)
        trip.itineraryItems.add(dinnerItem)
        trip.wishlistItems.add(WishlistItem(trip, place, ownerMembership))

        val expense = Expense(
            trip = trip,
            itineraryItem = dinnerItem,
            createdBy = owner,
            payer = ownerMembership,
            title = "Seed Dinner",
            amount = BigDecimal("3000"),
            currencyCode = "JPY",
            spentAt = trip.startDate,
            category = ExpenseCategory.FOOD,
            splitType = ExpenseSplitType.EQUAL,
            inputMethod = InputMethod.HANDWRITE,
        )
        val expenseItem = ExpenseItem(expense, "라멘", BigDecimal("3000"))
        expenseItem.assignments.add(ExpenseAssignment(expenseItem, ownerMembership, BigDecimal("1500")))
        expenseItem.assignments.add(ExpenseAssignment(expenseItem, memberMembership, BigDecimal("1500")))
        expense.expenseItems.add(expenseItem)

        tripRepository.save(trip)
        expenseRepository.save(expense)
        wishlistItemRepository.saveAll(trip.wishlistItems)

        logger.info("Local seed created. tripTitle={} ownerEmail={}", trip.title, owner.email)
    }
}
