# Backend Agent Guide

This file applies to work under `backend/`. If Codex was launched from the repository root, read this file before touching backend files.

## Stack

- Kotlin
- Spring Boot
- Gradle multi-module project under `backend/`
- Modules: `api`, `application`, `domain`, `batch`

## Required Context

- API and realtime contract: `../docs/contracts/api.md`
- Error contract: `../docs/contracts/errors.md`
- Fixture contract: `../docs/contracts/fixtures.md` when seed or test data changes
- Local harness: `../docs/runbooks/local-harness.md`
- Git workflow: `../docs/runbooks/git-flow.md`

## Backend Harness Rules

- Add or update tests before broad feature expansion.
- Keep tests at the narrowest useful layer: domain, application, API/controller, then integration.
- API response, realtime, and error shape changes must update the matching contract docs.
- Do not add a new backend production dependency without explicit user request.

## Module Boundaries

- `api` owns HTTP controllers, transport DTOs, security/web configuration, WebSocket adapters, and external HTTP-facing concerns.
- `application` owns use-case services, commands, results, gateways, policies, and orchestration.
- `domain` owns entities, repositories, domain enums, and persistence-domain logic.
- `batch` owns scheduled/background batch behavior.

## DTO and Use-case Model Rules

Follow the current Tribe structure. Do not invent ad hoc `Dto` layers or generic `controller/dto` trees for new features.

- API request DTOs live in feature-scoped `*Requests`.
- API response DTOs live in feature-scoped `*Responses`.
- Application input models live in `*Command`.
- Application output models live in `*Result`.
- Controllers map request DTOs into application commands.
- Controllers map application results into response DTOs.
- Application models must not depend on servlet APIs, controller types, HTTP annotations, or transport-only field names.

Examples from the current codebase:

- `com.tribe.api.auth.AuthRequests`
- `com.tribe.api.auth.AuthResponses`
- `com.tribe.application.auth.AuthCommand`
- `com.tribe.application.auth.AuthResult`

## Comment Rules

Follow `../docs/backend-comment-conventions.md` when adding or updating backend comments.

## Commands

Run from `backend/`:

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

Use `./gradlew :api:bootRun` only when local runtime behavior must be inspected.

## Completion Report

Backend changes should report:

- test targets executed
- contract docs updated or not applicable
- skipped checks and reason
- residual risk
