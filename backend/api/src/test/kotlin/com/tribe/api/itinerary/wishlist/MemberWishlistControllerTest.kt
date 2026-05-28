package com.tribe.api.itinerary.wishlist

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.itinerary.wishlist.MemberWishlistCommand
import com.tribe.application.itinerary.wishlist.MemberWishlistResult
import com.tribe.application.itinerary.wishlist.MemberWishlistService
import com.tribe.application.security.TokenProvider
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(MemberWishlistController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class MemberWishlistControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var memberWishlistService: MemberWishlistService
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `addWishlistItem returns created account wishlist payload`() {
        `when`(
            memberWishlistService.addWishlistItem(
                MemberWishlistCommand.Add(
                    externalPlaceId = "tokyo-tower",
                    placeName = "도쿄타워",
                    address = "도쿄",
                    latitude = BigDecimal.ONE,
                    longitude = BigDecimal.TEN,
                ),
            ),
        ).thenReturn(sampleItem())

        mockMvc.perform(
            post("/api/v1/members/me/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalPlaceId": "tokyo-tower",
                      "placeName": "도쿄타워",
                      "address": "도쿄",
                      "latitude": 1,
                      "longitude": 10
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.memberWishlistItemId", equalTo(1)))
            .andExpect(jsonPath("$.data.placeId", equalTo(10)))
            .andExpect(jsonPath("$.data.externalPlaceId", equalTo("tokyo-tower")))
            .andExpect(jsonPath("$.data.adder").doesNotExist())
    }

    @Test
    fun `getWishlistItems returns paged response without adder`() {
        `when`(memberWishlistService.getWishlist(PageRequest.of(0, 10)))
            .thenReturn(samplePage())

        mockMvc.perform(get("/api/v1/members/me/wishlists?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].memberWishlistItemId", equalTo(1)))
            .andExpect(jsonPath("$.data.content[0].placeId", equalTo(10)))
            .andExpect(jsonPath("$.data.content[0].externalPlaceId", equalTo("tokyo-tower")))
            .andExpect(jsonPath("$.data.content[0].adder").doesNotExist())
            .andExpect(jsonPath("$.data.totalElements", equalTo(1)))
    }

    @Test
    fun `getWishlistItems with query delegates search`() {
        `when`(memberWishlistService.searchWishlist("도쿄", PageRequest.of(0, 10)))
            .thenReturn(samplePage())

        mockMvc.perform(get("/api/v1/members/me/wishlists?query=도쿄&page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].name", equalTo("도쿄타워")))
    }

    @Test
    fun `deleteWishlistItems accepts memberWishlistItemIds`() {
        mockMvc.perform(
            delete("/api/v1/members/me/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"memberWishlistItemIds":[1,2]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist())

        verify(memberWishlistService).deleteWishlistItems(MemberWishlistCommand.Delete(listOf(1L, 2L)))
    }

    @Test
    fun `addWishlistItem rejects blank place name`() {
        mockMvc.perform(
            post("/api/v1/members/me/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalPlaceId": "tokyo-tower",
                      "placeName": " ",
                      "address": "도쿄",
                      "latitude": 35.6586,
                      "longitude": 139.7454
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))

        verifyNoInteractions(memberWishlistService)
    }

    @Test
    fun `addWishlistItem rejects out of range longitude`() {
        mockMvc.perform(
            post("/api/v1/members/me/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalPlaceId": "tokyo-tower",
                      "placeName": "도쿄타워",
                      "address": "도쿄",
                      "latitude": 35.6586,
                      "longitude": 181
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))

        verifyNoInteractions(memberWishlistService)
    }

    @Test
    fun `deleteWishlistItems rejects empty id list`() {
        mockMvc.perform(
            delete("/api/v1/members/me/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"memberWishlistItemIds":[]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))

        verifyNoInteractions(memberWishlistService)
    }

    @Test
    fun `deleteWishlistItems rejects non positive ids`() {
        mockMvc.perform(
            delete("/api/v1/members/me/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"memberWishlistItemIds":[0]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))

        verifyNoInteractions(memberWishlistService)
    }

    private fun samplePage() = MemberWishlistResult.SearchPage(
        content = listOf(sampleItem()),
        pageNumber = 0,
        pageSize = 10,
        totalPages = 1,
        totalElements = 1,
        isLast = true,
    )

    private fun sampleItem() = MemberWishlistResult.Item(
        memberWishlistItemId = 1L,
        placeId = 10L,
        externalPlaceId = "tokyo-tower",
        name = "도쿄타워",
        address = "도쿄",
        latitude = BigDecimal.ONE,
        longitude = BigDecimal.TEN,
        placeTypeSummary = null,
        normalizedCategoryKey = null,
        photoHint = null,
        placeDetailSummary = null,
    )
}
