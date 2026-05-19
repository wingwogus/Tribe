# Local Harness Runbook

This runbook is for executing repeatable local verification, not for storing design rationale.

## Frontend Checks

From `frontend/`:

```bash
npm run lint
npm run typecheck
npm run build
```

Use `npm run dev` only when local runtime or visual behavior must be inspected. The default development server is `http://localhost:8081`.

## Backend Checks

From `backend/`:

```bash
./gradlew test
```

Focused checks:

```bash
./gradlew :domain:test
./gradlew :application:test
./gradlew :api:test
./gradlew :batch:test
```

Use `./gradlew :api:bootRun` only when runtime behavior must be inspected.

## Full Local Runtime

Frontend development server:

```bash
cd frontend
npm run dev
```

Backend runtime:

```bash
cd backend
./gradlew :api:bootRun
```

The frontend Vite server proxies these paths to backend `http://localhost:8080`:

- `/api`
- `/oauth2`
- `/login`
- `/ws`

## Smoke Targets

Use the narrowest smoke that proves the changed behavior.

Suggested local targets when the relevant services are available:

- frontend route load at `http://localhost:8081`
- representative REST call under `/api/v1`
- WebSocket connection to `/ws`
- trip event topic `/sub/trips/{tripId}`
- chat event topic `/sub/chat/rooms/{tripId}`
- backend actuator health only when actuator endpoints are enabled in the target profile

## Completion Report

Every harness run should report:

- command executed
- pass/fail result
- skipped checks and reason
- manual smoke coverage, if applicable
- known residual risk
