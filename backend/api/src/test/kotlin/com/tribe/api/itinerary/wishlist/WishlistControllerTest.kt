package com.tribe.api.itinerary.wishlist

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.itinerary.place.OpeningSummary
import com.tribe.application.itinerary.place.OpeningSummarySource
import com.tribe.application.itinerary.wishlist.WishlistCommand
import com.tribe.application.itinerary.wishlist.WishlistResult
import com.tribe.application.itinerary.wishlist.WishlistService
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
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(WishlistController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class WishlistControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var wishlistService: WishlistService
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `getWishlistItems delegates sort parameter and returns like summary`() {
        `when`(wishlistService.getWishList(5L, PageRequest.of(0, 10, Sort.by("like_count_desc")), "like_count_desc"))
            .thenReturn(samplePage())

        mockMvc.perform(get("/api/v1/trips/5/wishlists?page=0&size=10&sort=like_count_desc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].wishlistItemId", equalTo(1)))
            .andExpect(jsonPath("$.data.content[0].externalPlaceId", equalTo("tokyo-tower")))
            .andExpect(jsonPath("$.data.content[0].photoHint.name", equalTo("places/tokyo-tower/photos/photo-1")))
            .andExpect(jsonPath("$.data.content[0].photoHint.photoUri").doesNotExist())
            .andExpect(jsonPath("$.data.content[0].openingSummary.openNow", equalTo(true)))
            .andExpect(jsonPath("$.data.content[0].openingSummary.source", equalTo("CURRENT")))
            .andExpect(jsonPath("$.data.content[0].openingSummary.stale", equalTo(false)))
            .andExpect(jsonPath("$.data.content[0].likeCount", equalTo(2)))
            .andExpect(jsonPath("$.data.content[0].likedByMe", equalTo(true)))
    }

    @Test
    fun `getWishlistItems delegates wishlistSort without pageable sort pollution`() {
        `when`(wishlistService.getWishList(5L, PageRequest.of(0, 10), "like_count_desc"))
            .thenReturn(samplePage())

        mockMvc.perform(get("/api/v1/trips/5/wishlists?page=0&size=10&wishlistSort=like_count_desc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(wishlistService).getWishList(5L, PageRequest.of(0, 10), "like_count_desc")
    }

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
            .andExpect(jsonPath("$.data.externalPlaceId", equalTo("tokyo-tower")))
            .andExpect(jsonPath("$.data.name", equalTo("도쿄타워")))
            .andExpect(jsonPath("$.data.photoHint.name", equalTo("places/tokyo-tower/photos/photo-1")))
            .andExpect(jsonPath("$.data.openingSummary.source", equalTo("CURRENT")))
            .andExpect(jsonPath("$.data.adder.tripMemberId", equalTo(3)))
            .andExpect(jsonPath("$.data.adder.nickname", equalTo("member")))
            .andExpect(jsonPath("$.data.adder.avatar", equalTo("https://cdn.example.com/member.png")))
    }

    @Test
    fun `likeWishlistItem delegates like command`() {
        `when`(wishlistService.likeWishlistItem(WishlistCommand.Like(5L, 7L)))
            .thenReturn(WishlistResult.LikeSummary(likeCount = 3L, likedByMe = true))

        mockMvc.perform(post("/api/v1/trips/5/wishlists/7/likes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.likeCount", equalTo(3)))
            .andExpect(jsonPath("$.data.likedByMe", equalTo(true)))

        verify(wishlistService).likeWishlistItem(WishlistCommand.Like(5L, 7L))
    }

    @Test
    fun `unlikeWishlistItem delegates unlike command`() {
        `when`(wishlistService.unlikeWishlistItem(WishlistCommand.Like(5L, 7L)))
            .thenReturn(WishlistResult.LikeSummary(likeCount = 2L, likedByMe = false))

        mockMvc.perform(delete("/api/v1/trips/5/wishlists/7/likes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.likeCount", equalTo(2)))
            .andExpect(jsonPath("$.data.likedByMe", equalTo(false)))

        verify(wishlistService).unlikeWishlistItem(WishlistCommand.Like(5L, 7L))
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

    private fun samplePage() = WishlistResult.SearchPage(
        content = listOf(sampleItem()),
        pageNumber = 0,
        pageSize = 10,
        totalPages = 1,
        totalElements = 1,
        isLast = true,
    )

    private fun sampleItem() = WishlistResult.Item(
        wishlistItemId = 1L,
        placeId = 10L,
        externalPlaceId = "tokyo-tower",
        name = "도쿄타워",
        address = "도쿄",
        latitude = BigDecimal.ONE,
        longitude = BigDecimal.TEN,
        placeTypeSummary = null,
        normalizedCategoryKey = null,
        photoHint = WishlistResult.PhotoHint("places/tokyo-tower/photos/photo-1", null),
        placeDetailSummary = null,
        openingSummary = OpeningSummary(
            openNow = true,
            nextOpenTime = null,
            nextCloseTime = "2026-05-17T22:00:00+09:00",
            source = OpeningSummarySource.CURRENT,
            timezoneOffsetMinutes = 540,
            syncedAt = LocalDateTime.of(2026, 5, 17, 10, 0),
            stale = false,
        ),
        adder = WishlistResult.Adder(
            tripMemberId = 3L,
            memberId = 2L,
            nickname = "member",
            avatar = "https://cdn.example.com/member.png",
        ),
        likeCount = 2L,
        likedByMe = true,
    )
}
