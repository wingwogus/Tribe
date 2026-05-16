package com.tribe.domain.itinerary.wishlist

enum class AccountWishlistSort(
    val apiValue: String,
    private val aliases: Set<String> = emptySet(),
) {
    RATING_DESC("rating_desc"),
    REVIEW_COUNT_DESC("review_count_desc"),
    /** Sort by rating first, then by review count as a confidence tie-breaker. */
    REVIEW_GOOD_DESC("review_good_desc", setOf("review_good")),
    ;

    companion object {
        fun fromApiValue(value: String?): AccountWishlistSort? {
            val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.apiValue == normalized || normalized in it.aliases }
        }
    }
}
