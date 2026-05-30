package com.tribe.api.trip.core

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.TokenProvider
import com.tribe.application.trip.core.TripCommand
import com.tribe.application.trip.core.TripResult
import com.tribe.application.trip.core.TripService
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(TripController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class TripControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    companion object {
        private const val REGION_CODE = "JP_TOKYO"
    }

    @MockBean
    private lateinit var tripService: TripService

    @MockBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `createTrip accepts region code`() {
        `when`(
            tripService.createTrip(
                TripCommand.Create(
                    "Trip",
                    LocalDate.of(2026, 4, 12),
                    LocalDate.of(2026, 4, 13),
                    "JP",
                    "JP_TOKYO",
                ),
            ),
        ).thenReturn(sampleTripDetail())

        mockMvc.perform(
            post("/api/v1/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Trip",
                      "startDate": "2026-04-12",
                      "endDate": "2026-04-13",
                      "country": "JP",
                      "regionCode": "JP_TOKYO"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.country", equalTo("JP")))
            .andExpect(jsonPath("$.data.regionCode", equalTo("JP_TOKYO")))
    }

    @Test
    fun `getAllTrips returns localized country in summary response`() {
        `when`(tripService.getAllTrips(PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startDate"))))
            .thenReturn(
                PageImpl(
                    listOf(
                        TripResult.SimpleTrip(
                            tripId = 5L,
                            title = "Trip",
                            startDate = LocalDate.of(2026, 4, 12),
                            endDate = LocalDate.of(2026, 4, 13),
                            country = "일본",
                            regionCode = "JP_TOKYO",
                            memberCount = 2,
                            members = listOf(
                                TripResult.MemberSummary(
                                    tripMemberId = 1L,
                                    memberId = 2L,
                                    nickname = "member",
                                    avatar = "https://cdn.example.com/member.png",
                                    role = "OWNER",
                                ),
                            ),
                        ),
                    ),
                    PageRequest.of(0, 10),
                    1,
                ),
            )

        mockMvc.perform(get("/api/v1/trips"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].country", equalTo("일본")))
            .andExpect(jsonPath("$.data[0].regionCode", equalTo("JP_TOKYO")))
            .andExpect(jsonPath("$.data[0].members[0].avatar", equalTo("https://cdn.example.com/member.png")))
    }

    @Test
    fun `addGuest returns created trip detail`() {
        `when`(tripService.addGuest(TripCommand.AddGuest(5L, "guest"))).thenReturn(sampleTripDetail())

        mockMvc.perform(
            post("/api/v1/trips/5/guests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"guest"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.regionCode", equalTo(REGION_CODE)))
            .andExpect(jsonPath("$.data.members[0].nickname", equalTo("guest")))
            .andExpect(jsonPath("$.data.members[0].role", equalTo("GUEST")))
    }

    @Test
    fun `createTrip forwards optional regionCode`() {
        `when`(
            tripService.createTrip(
                TripCommand.Create(
                    "Trip",
                    LocalDate.of(2026, 4, 12),
                    LocalDate.of(2026, 4, 13),
                    "JP",
                    REGION_CODE,
                ),
            ),
        ).thenReturn(sampleTripDetail())

        mockMvc.perform(
            post("/api/v1/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Trip",
                      "startDate": "2026-04-12",
                      "endDate": "2026-04-13",
                      "country": "JP",
                      "regionCode": "$REGION_CODE"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.regionCode", equalTo(REGION_CODE)))
    }

    @Test
    fun `updateTrip forwards destructive confirmation flag`() {
        `when`(
            tripService.updateTrip(
                TripCommand.Update(
                    tripId = 5L,
                    title = "Updated",
                    startDate = LocalDate.of(2026, 4, 12),
                    endDate = LocalDate.of(2026, 4, 13),
                    country = "JP",
                    regionCode = REGION_CODE,
                    deleteOutOfRangeItems = true,
                ),
            ),
        ).thenReturn(sampleTripDetail())

        mockMvc.perform(
            patch("/api/v1/trips/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Updated",
                      "startDate": "2026-04-12",
                      "endDate": "2026-04-13",
                      "country": "JP",
                      "regionCode": "$REGION_CODE",
                      "deleteOutOfRangeItems": true
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `updateTrip returns conflict detail when date shrink needs item deletion`() {
        `when`(
            tripService.updateTrip(
                TripCommand.Update(
                    tripId = 5L,
                    title = "Updated",
                    startDate = LocalDate.of(2026, 4, 12),
                    endDate = LocalDate.of(2026, 4, 13),
                    country = "JP",
                    regionCode = REGION_CODE,
                ),
            ),
        ).thenThrow(
            BusinessException(
                errorCode = ErrorCode.TRIP_DATE_RANGE_REQUIRES_ITEM_DELETION,
                detail = mapOf(
                    "outOfRangeItemCount" to 1,
                    "newTotalDays" to 2,
                    "outOfRangeItems" to listOf(mapOf("itemId" to 10L, "visitDay" to 3, "title" to "Dinner")),
                ),
            ),
        )

        mockMvc.perform(
            patch("/api/v1/trips/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Updated",
                      "startDate": "2026-04-12",
                      "endDate": "2026-04-13",
                      "country": "JP",
                      "regionCode": "$REGION_CODE"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("TRIP_008")))
            .andExpect(jsonPath("$.error.message", equalTo("error.trip_date_range_requires_item_deletion")))
            .andExpect(jsonPath("$.error.detail.outOfRangeItemCount", equalTo(1)))
            .andExpect(jsonPath("$.error.detail.newTotalDays", equalTo(2)))
    }

    @Test
    fun `assignRole rejects blank role`() {
        mockMvc.perform(
            patch("/api/v1/trips/5/members/2/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":" "}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
    }

    @Test
    fun `leaveTrip returns updated trip detail`() {
        `when`(tripService.leaveTrip(TripCommand.Leave(5L))).thenReturn(sampleTripDetail())

        mockMvc.perform(delete("/api/v1/trips/5/members/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.tripId", equalTo(5)))
    }

    @Test
    fun `importTrip returns imported trip detail`() {
        `when`(
            tripService.importTrip(
                TripCommand.Import(
                    9L,
                    "Imported",
                    LocalDate.of(2026, 4, 12),
                    LocalDate.of(2026, 4, 13),
                )
            )
        ).thenReturn(sampleTripDetail())

        mockMvc.perform(
            post("/api/v1/trips/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "postId": 9,
                      "title": "Imported",
                      "startDate": "2026-04-12",
                      "endDate": "2026-04-13"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tripId", equalTo(5)))
            .andExpect(jsonPath("$.data.country", equalTo("JP")))
    }

    private fun sampleTripDetail() = TripResult.TripDetail(
        tripId = 5L,
        title = "Trip",
        startDate = LocalDate.of(2026, 4, 12),
        endDate = LocalDate.of(2026, 4, 13),
        country = "JP",
        regionCode = REGION_CODE,
        members = listOf(
            TripResult.MemberSummary(
                tripMemberId = 1L,
                memberId = null,
                nickname = "guest",
                avatar = null,
                role = "GUEST",
            ),
        ),
    )
}
