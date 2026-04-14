package com.tribe.api.itinerary.item

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.itinerary.item.ItemCommand
import com.tribe.application.itinerary.item.ItemResult
import com.tribe.application.itinerary.item.ItemService
import com.tribe.application.itinerary.place.PlaceSearchResult
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.application.security.TokenProvider
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(ItemController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ItemControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var itemService: ItemService
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `createItem returns created payload`() {
        `when`(
            itemService.createItem(
                ItemCommand.Create(
                    tripId = 5L,
                    visitDay = 1,
                    title = "Dinner",
                    time = LocalDateTime.of(2026, 4, 12, 19, 0),
                    memo = "Booked",
                ),
            ),
        ).thenReturn(sampleItemView())

        mockMvc.perform(
            post("/api/v1/trips/5/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "visitDay": 1,
                      "title": "Dinner",
                      "time": "2026-04-12T19:00:00",
                      "memo": "Booked"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.itemId", equalTo(1)))
            .andExpect(jsonPath("$.data.visitDay", equalTo(1)))
    }

    @Test
    fun `getAllItems returns collection`() {
        `when`(itemService.getAllItems(5L, null)).thenReturn(listOf(sampleItemView()))

        mockMvc.perform(get("/api/v1/trips/5/items"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].itemId", equalTo(1)))
            .andExpect(jsonPath("$.data[0].title", equalTo("Dinner")))
    }

    @Test
    fun `updateItemOrder returns reordered items`() {
        `when`(
            itemService.updateItemOrder(
                ItemCommand.OrderUpdate(
                    tripId = 5L,
                    items = listOf(
                        ItemCommand.OrderItem(
                            itemId = 1L,
                            visitDay = 2,
                            itemOrder = 2,
                        ),
                    ),
                ),
            ),
        ).thenReturn(listOf(sampleItemView(visitDay = 2, itemOrder = 2)))

        mockMvc.perform(
            patch("/api/v1/trips/5/items/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"items":[{"itemId":1,"visitDay":2,"itemOrder":2}]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].visitDay", equalTo(2)))
            .andExpect(jsonPath("$.data[0].itemOrder", equalTo(2)))
    }

    @Test
    fun `getAllDirections returns route collection`() {
        `when`(itemService.getAllDirectionsForTrip(5L, "walking")).thenReturn(
            listOf(
                RouteDetails(
                    travelMode = "WALKING",
                    originPlace = PlaceSearchResult("origin", "Origin", "addr1", 0.0, 0.0),
                    destinationPlace = PlaceSearchResult("dest", "Destination", "addr2", 1.0, 1.0),
                    totalDuration = "10 mins",
                    totalDistance = "1 km",
                    steps = emptyList(),
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/trips/5/items/directions?mode=walking"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].travelMode", equalTo("WALKING")))
            .andExpect(jsonPath("$.data[0].originPlace.externalPlaceId", equalTo("origin")))
    }

    private fun sampleItemView(
        visitDay: Int = 1,
        itemOrder: Int = 3,
    ) = ItemResult.ItemView(
        itemId = 1L,
        tripId = 5L,
        visitDay = visitDay,
        itemOrder = itemOrder,
        placeId = null,
        externalPlaceId = null,
        name = "Dinner",
        title = "Dinner",
        time = LocalDateTime.of(2026, 4, 12, 19, 0),
        memo = "Booked",
        location = null,
        placeTypeSummary = null,
        openingStatusWarning = null,
    )
}
