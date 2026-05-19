# Fixture Contracts

Fixtures define stable product context for tests and optional local backend seed data.

## Runtime Data Policy

- Frontend production runtime must not load fixture-backed, mock-backed, or hard-coded product records.
- Frontend screens must load trip, itinerary, place, chat, expense, settlement, review, community, and member data through the backend API and realtime contracts in `docs/contracts/api.md`.
- Fixture JSON may be transformed into persisted backend seed data for local development, but the app still consumes it only after the backend serves it through HTTP/WebSocket surfaces.
- Any future offline cache must be populated from successful backend responses, not bundled fixture files.

## Fixture Identity Rules

- IDs must be stable across test runs.
- Names should be readable enough for failing tests.
- Emails must be fake and reserved for test use.
- Do not include real user data, real secrets, real access tokens, or private travel details.
- Time-dependent fixture values must be deterministic or explicitly normalized in tests.

## Scenario Rules

Every fixture addition should state the scenario it locks. Useful baseline scenarios include:

1. Anonymous user reads public community posts.
2. Authenticated member creates or joins a trip.
3. Trip member edits itinerary items.
4. Trip member sends and loads chat history.
5. Trip members record expenses and compute settlement.
6. User generates or views a trip review.

## Storage Format

No canonical shared fixture file exists yet. When introduced, shared fixtures should live under `docs/fixtures/` and be portable enough for backend tests and local seed flows.

Backend integration tests may transform shared fixture data into persisted entities during setup. Frontend app targets must not bundle or decode shared fixture JSON directly in production paths.

## Harness Requirement

When fixture or seed data changes:

- Update this document if identity, scenario, or runtime-data policy changes.
- Add or update tests that prove the fixture scenario.
- Keep fixture values deterministic and scrubbed of real user data.
- Document any manual local seed smoke in `docs/runbooks/local-harness.md`.
