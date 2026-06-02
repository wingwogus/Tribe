# Error Contracts

All public API errors must fit the shared response envelope:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "message.key.or.localized.message",
    "detail": null
  }
}
```

Initial source files:

- `backend/application/src/main/kotlin/com/tribe/application/exception/ErrorCode.kt`
- `backend/api/src/main/kotlin/com/tribe/api/exception/GlobalExceptionHandler.kt`

## Rules

- `code` must be stable enough for frontend branching.
- `message` should be user-facing or localization-key compatible.
- `detail` is optional and must not leak secrets, token values, stack traces, provider credentials, or private user data.
- Validation errors should keep a predictable field-level shape in `detail`.
- JSON parse and unexpected errors may include an `eventId` for log correlation, but not raw exception text.

## Current Stable Codes

Common:

- `COMMON_001`: invalid input
- `COMMON_002`: invalid JSON
- `COMMON_010`: image upload failed
- `COMMON_011`: external API error
- `COMMON_012`: AI feedback error
- `COMMON_999`: internal error
- `RESOURCE_001`: resource not found

Auth and member:

- `AUTH_001`: unauthorized
- `AUTH_002`: forbidden
- `AUTH_003`: duplicate email
- `AUTH_004`: email not verified
- `AUTH_005`: auth code not found
- `AUTH_006`: auth code mismatch
- `AUTH_007`: already logged out
- `USER_001`: user not found
- `USER_002`: user already exists

Trip:

- `TRIP_001`: trip not found
- `TRIP_002`: not a trip member
- `TRIP_003`: no trip authority
- `TRIP_004`: invalid invite token
- `TRIP_005`: already joined trip
- `TRIP_006`: banned member
- `TRIP_007`: trip review not found
- `TRIP_008`: trip date range shrink requires confirmed itinerary item deletion

Community:

- `COMMUNITY_001`: post not found

Itinerary and places:

- `ITINERARY_001`: category not found
- `ITINERARY_002`: item not found
- `ITINERARY_003`: trip ownership mismatch
- `ITINERARY_004`: invalid input value
- `ITINERARY_005`: wishlist item already exists
- `ITINERARY_006`: wishlist item not found
- `ITINERARY_007`: duplicate category id request
- `ITINERARY_008`: duplicate order request
- `ITINERARY_009`: category day mismatch
- `ITINERARY_010`: place not found

Exchange:

- `EXCHANGE_001`: exchange rate not found

## Validation Detail Shape

Validation errors currently expose the first field error as:

```json
{
  "field": "fieldName",
  "reason": "Invalid value",
  "rejectedValue": "bad-value"
}
```

Keep this shape predictable if more validation details are added.

## Trip Date Shrink Conflict Detail

`PATCH /api/v1/trips/{tripId}` returns `TRIP_008` with HTTP 409 when the requested date range would remove days that still contain itinerary items and `deleteOutOfRangeItems` is omitted or false.

```json
{
  "outOfRangeItemCount": 1,
  "newTotalDays": 2,
  "outOfRangeItems": [
    {
      "itemId": 10,
      "visitDay": 3,
      "title": "Dinner"
    }
  ]
}
```

The frontend may retry the same update with `deleteOutOfRangeItems: true` after user confirmation. The retry must be treated as destructive: the backend updates the trip and deletes those out-of-range itinerary items in the same transaction.

## Harness Requirement

When an error shape or code changes:

- Update this document.
- Update backend tests for the controller or exception path.
- Update frontend handling when UI behavior branches on the code or detail shape.
- Confirm `detail` does not expose secrets, tokens, stack traces, or private user data.
