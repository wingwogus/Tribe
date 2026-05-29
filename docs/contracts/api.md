# API and Realtime Contracts

This document is the shared API and realtime contract surface for frontend and backend work.

## Runtime Data Rule

Frontend runtime screens must load product data through backend REST APIs and WebSocket/STOMP topics. Do not ship fixture-backed, mock-backed, or hard-coded trip, itinerary, chat, expense, settlement, review, place, or member records in production UI paths.

Test-only doubles may validate rendering, decoding, and request composition. Local fixture or seed data becomes app-visible only after the backend persists and serves it through the normal API surface.

## Response Envelope

Successful public API responses use:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

Failure responses use:

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

The Kotlin source of truth is `backend/api/src/main/kotlin/com/tribe/api/common/ApiResponse.kt`.

## HTTP Contract Groups

Current route groups include:

### Trip

- `POST /api/v1/trips`
- `POST /api/v1/trips/{tripId}/invite`
- `POST /api/v1/trips/join`
- `POST /api/v1/trips/import`
- Trip member payloads returned from trip summary/detail endpoints include `avatar: string | null`.
- `GET /api/v1/trips` summary rows include `members[]` for rendering compact member profile stacks; `memberCount` remains the authoritative count.

### Itinerary and Places

- `POST /api/v1/trips/{tripId}/items`
- `PATCH /api/v1/trips/{tripId}/items/order`
- `GET /api/v1/trips/{tripId}/items/directions`
- `GET /api/v1/places/search`
- `POST /api/v1/places/nearby`
- `POST /api/v1/places/resolve`
- `POST /api/v1/trips/{tripId}/wishlists`
- `POST /api/v1/trips/{tripId}/wishlists/from-place`
- `POST /api/v1/trips/{tripId}/wishlists/from-member-wishlist`
- `GET /api/v1/trips/{tripId}/wishlists`
- `DELETE /api/v1/trips/{tripId}/wishlists`
- Wishlist item `adder` payloads include `avatar: string | null`.

### Expense and Settlement

- `POST /api/v1/trips/{tripId}/expenses`
- `GET /api/v1/trips/{tripId}/settlements/daily`
- `GET /api/v1/trips/{tripId}/settlements/total`

### Chat, Review, and Community

- `POST /api/v1/trips/{tripId}/chat`
- `GET /api/v1/trips/{tripId}/chat`
- `POST /api/v1/trips/{tripId}/reviews`
- `POST /api/v1/community/posts`

Keep this section aligned with controllers and frontend API wrappers when public route shapes change.

## Realtime Contract

WebSocket transport uses SockJS + STOMP.

- Endpoint: `${VITE_BACKEND_ORIGIN}/ws`
- Trip event topic: `/sub/trips/{tripId}`
- Chat event topic: `/sub/chat/rooms/{tripId}`

Frontend realtime handlers should invalidate or update React Query caches consistently with REST reads.

## Backend Shape Rules

- Controllers own HTTP request/response mapping.
- API request DTOs live in `*Requests`.
- API response DTOs live in `*Responses`.
- Application services accept `*Command` inputs and return `*Result` outputs.
- Application models must not depend on HTTP annotations, controller types, cookies, or servlet APIs.

## Harness Requirement

When API or realtime shape changes:

- Update this document.
- Add or update backend controller/API tests when the backend contract changes.
- Update frontend API wrappers, types, and query/cache behavior when the client contract changes.
- Document any manual smoke that could not be automated.
