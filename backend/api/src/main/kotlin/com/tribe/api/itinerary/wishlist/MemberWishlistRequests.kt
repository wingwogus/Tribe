package com.tribe.api.itinerary.wishlist

import com.tribe.application.itinerary.wishlist.MemberWishlistCommand
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * 위시리스트 HTTP request 모델 경계.
 *
 * controller 입력 shape와 application command 변환 기준.
 */
object MemberWishlistRequests {
    data class MemberWishlistAddRequest(
        @field:NotBlank(message = "외부 장소 ID는 필수입니다.")
        @field:Size(max = 255, message = "외부 장소 ID는 255자 이하여야 합니다.")
        val externalPlaceId: String,
        @field:NotBlank(message = "장소 이름은 필수입니다.")
        @field:Size(max = 255, message = "장소 이름은 255자 이하여야 합니다.")
        val placeName: String,
        @field:Size(max = 500, message = "주소는 500자 이하여야 합니다.")
        val address: String?,
        @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        val latitude: BigDecimal,
        @field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        val longitude: BigDecimal,
    ) {
        fun toCommand(): MemberWishlistCommand.Add = MemberWishlistCommand.Add(
            externalPlaceId = externalPlaceId,
            placeName = placeName,
            address = address,
            latitude = latitude,
            longitude = longitude,
        )
    }

    data class MemberWishlistDeleteRequest(
        @field:NotEmpty(message = "삭제할 개인 위시리스트 항목은 비어있을 수 없습니다.")
        @field:Size(max = 100, message = "한 번에 삭제할 수 있는 개인 위시리스트 항목은 100개 이하입니다.")
        val memberWishlistItemIds: List<Long>,
    ) {
        @AssertTrue(message = "개인 위시리스트 항목 ID는 양수여야 합니다.")
        fun hasOnlyPositiveMemberWishlistItemIds(): Boolean = memberWishlistItemIds.all { it > 0 }

        fun toCommand(): MemberWishlistCommand.Delete = MemberWishlistCommand.Delete(
            memberWishlistItemIds = memberWishlistItemIds,
        )
    }
}
