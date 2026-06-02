package com.tribe.application.itinerary.place

/**
 * 장소 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
enum class NearbyPlaceCategory {
    RESTAURANT,
    CAFE,
    BAKERY,
    BAR,
    ATTRACTION,
    SHOPPING,
    PARK,
    MUSEUM,
    STAY,
}
