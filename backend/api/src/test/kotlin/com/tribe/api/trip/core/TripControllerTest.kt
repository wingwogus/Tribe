package com.tribe.api.trip.core

import com.tribe.api.exception.GlobalExceptionHandler
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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
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
                role = "GUEST",
            ),
        ),
    )
}
