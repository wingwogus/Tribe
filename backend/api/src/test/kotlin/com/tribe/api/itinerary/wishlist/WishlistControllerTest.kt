package com.tribe.api.itinerary.wishlist

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.itinerary.wishlist.WishlistCommand
import com.tribe.application.itinerary.wishlist.WishlistResult
import com.tribe.application.itinerary.wishlist.WishlistService
import com.tribe.application.security.TokenProvider
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(WishlistController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class WishlistControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var wishlistService: WishlistService
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `addWishlistItemFromPlace returns created trip wishlist payload`() {
        `when`(
            wishlistService.addWishListFromPlace(
                WishlistCommand.AddFromPlace(
                    tripId = 5L,
                    placeId = 10L,
                ),
            ),
        ).thenReturn(sampleItem())

        mockMvc.perform(
            post("/api/v1/trips/5/wishlists/from-place")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"placeId":10}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.wishlistItemId", equalTo(1)))
            .andExpect(jsonPath("$.data.placeId", equalTo(10)))
            .andExpect(jsonPath("$.data.name", equalTo("도쿄타워")))
            .andExpect(jsonPath("$.data.adder.tripMemberId", equalTo(3)))
    }

    @Test
    fun `addWishlistItemFromMemberWishlist returns created trip wishlist payload`() {
        `when`(
            wishlistService.addWishListFromMemberWishlist(
                WishlistCommand.AddFromMemberWishlist(
                    tripId = 5L,
                    memberWishlistItemId = 30L,
                ),
            ),
        ).thenReturn(sampleItem())

        mockMvc.perform(
            post("/api/v1/trips/5/wishlists/from-member-wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"memberWishlistItemId":30}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.wishlistItemId", equalTo(1)))
            .andExpect(jsonPath("$.data.placeId", equalTo(10)))
            .andExpect(jsonPath("$.data.name", equalTo("도쿄타워")))
            .andExpect(jsonPath("$.data.adder.tripMemberId", equalTo(3)))
            .andExpect(jsonPath("$.data.adder.nickname", equalTo("member")))
            .andExpect(jsonPath("$.data.adder.avatar", equalTo("https://cdn.example.com/member.png")))
    }

    @Test
    fun `addWishlistItem rejects blank place identifiers`() {
        mockMvc.perform(
            post("/api/v1/trips/5/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalPlaceId": " ",
                      "placeName": "도쿄타워",
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

        verifyNoInteractions(wishlistService)
    }

    @Test
    fun `addWishlistItem rejects out of range latitude`() {
        mockMvc.perform(
            post("/api/v1/trips/5/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalPlaceId": "tokyo-tower",
                      "placeName": "도쿄타워",
                      "address": "도쿄",
                      "latitude": 91,
                      "longitude": 139.7454
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))

        verifyNoInteractions(wishlistService)
    }

    @Test
    fun `deleteWishlistItems rejects too many ids`() {
        val ids = (1..101).joinToString(prefix = "[", postfix = "]")

        mockMvc.perform(
            delete("/api/v1/trips/5/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"wishlistItemIds":$ids}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))

        verifyNoInteractions(wishlistService)
    }

    @Test
    fun `deleteWishlistItems rejects non positive ids`() {
        mockMvc.perform(
            delete("/api/v1/trips/5/wishlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"wishlistItemIds":[0]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))

        verifyNoInteractions(wishlistService)
    }

    private fun sampleItem() = WishlistResult.Item(
        wishlistItemId = 1L,
        placeId = 10L,
        name = "도쿄타워",
        address = "도쿄",
        latitude = BigDecimal.ONE,
        longitude = BigDecimal.TEN,
        placeTypeSummary = null,
        normalizedCategoryKey = null,
        photoHint = null,
        placeDetailSummary = null,
        adder = WishlistResult.Adder(
            tripMemberId = 3L,
            memberId = 2L,
            nickname = "member",
            avatar = "https://cdn.example.com/member.png",
        ),
    )
}
