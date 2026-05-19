# Frontend Agent Guide

This file applies to work under `frontend/`. If Codex was launched from the repository root, read this file before touching frontend files.

## Stack

- React 18
- TypeScript
- Vite
- React Router
- TanStack Query
- Tailwind CSS
- shadcn/ui
- SockJS + STOMP
- Leaflet

## Required Context

- API and realtime contract: `../docs/contracts/api.md`
- Error contract: `../docs/contracts/errors.md`
- Local harness: `../docs/runbooks/local-harness.md`
- Git workflow: `../docs/runbooks/git-flow.md`

## Frontend Harness Rules

- Runtime screens should consume backend data through `src/api/*` and WebSocket surfaces, not fixture-backed or hard-coded product data.
- Keep REST client behavior centralized in the existing API layer. Do not bypass `src/api/http.ts` for authenticated API calls without a documented reason.
- Keep React Query keys centralized around the existing query-key utilities, especially `src/lib/tripQueryKeys.ts`.
- WebSocket changes must account for REST fallback, cache invalidation, or reconnect behavior.
- Map, itinerary, and drag/drop changes need at least typecheck/build verification and a documented manual smoke when visual behavior matters.
- Reuse existing shadcn/ui, Tailwind, and lucide patterns. Do not add a new frontend production dependency without explicit user request.

## Commands

Run from `frontend/`:

```bash
npm run lint
npm run typecheck
npm run build
```

Use `npm run dev` only when local runtime or visual behavior must be inspected.

## Completion Report

Frontend changes should report:

- commands executed
- pass/fail result
- skipped checks and reason
- manual smoke coverage when UI, map, auth, or realtime behavior changes
- residual risk
