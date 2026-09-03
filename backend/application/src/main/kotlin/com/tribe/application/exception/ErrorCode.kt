package com.tribe.application.exception

/**
 * 예외 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
enum class ErrorCode(
    val code: String,
    val messageKey: String,
    val status: Int
) {

    INVALID_INPUT("COMMON_001", "error.invalid_input", 400),
    INVALID_JSON("COMMON_002", "error.invalid_json", 400),
    INTERNAL_ERROR("COMMON_999", "error.internal_error", 500),
    RESOURCE_NOT_FOUND("RESOURCE_001","error.resource_not_found", 404),
    USER_NOT_FOUND("USER_001", "error.user_not_found", 404),
    USER_ALREADY_EXISTS("USER_002", "error.user_already_exists", 409),
    DUPLICATE_EMAIL("AUTH_003", "error.duplicate_email", 409),
    EMAIL_NOT_VERIFIED("AUTH_004", "error.email_not_verified", 400),
    AUTH_CODE_NOT_FOUND("AUTH_005", "error.auth_code_not_found", 404),
    AUTH_CODE_MISMATCH("AUTH_006", "error.auth_code_mismatch", 400),
    ALREADY_LOGGED_OUT("AUTH_007", "error.already_logged_out", 400),
    UNAUTHORIZED("AUTH_001", "error.unauthorized", 401),
    FORBIDDEN("AUTH_002", "error.forbidden", 403),
    TRIP_NOT_FOUND("TRIP_001", "error.trip_not_found", 404),
    NOT_A_TRIP_MEMBER("TRIP_002", "error.not_a_trip_member", 403),
    NO_AUTHORITY_TRIP("TRIP_003", "error.no_authority_trip", 403),
    INVALID_INVITE_TOKEN("TRIP_004", "error.invalid_invite_token", 400),
    ALREADY_JOINED_TRIP("TRIP_005", "error.already_joined_trip", 409),
    BANNED_MEMBER("TRIP_006", "error.banned_member", 403),
    TRIP_REVIEW_NOT_FOUND("TRIP_007", "error.trip_review_not_found", 404),
    TRIP_DATE_RANGE_REQUIRES_ITEM_DELETION("TRIP_008", "error.trip_date_range_requires_item_deletion", 409),
    AI_FEEDBACK_ERROR("COMMON_012", "error.ai_feedback_error", 502),
    POST_NOT_FOUND("COMMUNITY_001", "error.post_not_found", 404),
    IMAGE_UPLOAD_FAILED("COMMON_010", "error.image_upload_failed", 500),
    CATEGORY_NOT_FOUND("ITINERARY_001", "error.category_not_found", 404),
    ITEM_NOT_FOUND("ITINERARY_002", "error.item_not_found", 404),
    NO_BELONG_TRIP("ITINERARY_003", "error.no_belong_trip", 400),
    INVALID_INPUT_VALUE("ITINERARY_004", "error.invalid_input_value", 400),
    WISHLIST_ITEM_ALREADY_EXISTS("ITINERARY_005", "error.wishlist_item_already_exists", 409),
    WISHLIST_ITEM_NOT_FOUND("ITINERARY_006", "error.wishlist_item_not_found", 404),
    DUPLICATE_CATEGORY_ID_REQUEST("ITINERARY_007", "error.duplicate_category_id_request", 400),
    DUPLICATE_ORDER_REQUEST("ITINERARY_008", "error.duplicate_order_request", 400),
    CATEGORY_DAY_MISMATCH("ITINERARY_009", "error.category_day_mismatch", 400),
    PLACE_NOT_FOUND("ITINERARY_010", "error.place_not_found", 404),
    WISHLIST_ITEM_LIKE_ALREADY_EXISTS("ITINERARY_011", "error.wishlist_item_like_already_exists", 409),
    EXTERNAL_API_ERROR("COMMON_011", "error.external_api_error", 502),
    EXCHANGE_RATE_NOT_FOUND("EXCHANGE_001", "error.exchange_rate_not_found", 404),

}
