package com.tribe.domain.itinerary.wishlist

enum class TripWishlistSort(
    val apiValue: String,
    private val aliases: Set<String> = emptySet(),
) {
    RATING_DESC("rating_desc"),
    REVIEW_COUNT_DESC("review_count_desc"),
    /** Sort by rating first, then by review count as a confidence tie-breaker. */
    REVIEW_GOOD_DESC("review_good_desc", setOf("review_good")),
    LIKE_COUNT_DESC("like_count_desc"),
    LIKE_COUNT_ASC("like_count_asc"),
    ;

    companion object {
        fun fromApiValue(value: String?): TripWishlistSort? {
            val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.apiValue == normalized || normalized in it.aliases }
        }
    }
}
