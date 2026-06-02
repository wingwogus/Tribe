# Backend Comment Conventions

This document defines how agents and contributors should write backend comments in Tribe.

## Purpose

Backend comments should help a new developer understand flow, boundaries, hidden framework behavior, external integration assumptions, and data lifecycle decisions.

Comments are not for restating obvious Kotlin syntax, DTO field names, repository names, or annotations.

## Language

- Markdown guidance stays in English so AI agents can consume it reliably.
- Kotlin source comments are Korean-first.
- Korean source comments should use concise descriptive endings such as `확정`, `보강`, `병합`, `위임`, `유지`, `보관`, `조립`, `분리`, `제한`.
- Avoid verbose declarative endings in source comments, especially `~한다`, `~이다`, `합니다`, `입니다`, and `다.`.

## KDoc vs Ordinary Comments

Use KDoc for stable contracts:

- Controller, service, gateway, repository, policy, entity, and configuration roles.
- Public use-case boundaries and integration ports.
- Result, command, event, and payload model responsibilities.

Use ordinary comments for local reasoning:

- Multi-step orchestration.
- Validation and normalization choices.
- Cache keys and idempotency.
- External API field masks and mapping decisions.
- Transaction boundaries, concurrency handling, and retry-safe paths.
- Framework callbacks where execution order is not obvious.

## Density

Prefer comments at meaningful blocks instead of every line.

Flow-heavy files need enough comments for a first-time reader to follow the sequence. Simple DTO, enum, and repository files usually need only a short KDoc boundary comment.

## Examples

Good:

```kotlin
// 흐름: 입력 검증 -> 캐시 조회 -> 외부 후보 조회 -> canonical 장소 병합 순서 확정.
```

Good:

```kotlin
/**
 * 장소 검색 use case.
 *
 * 외부 Google 후보와 내부 canonical `Place` 병합 경계.
 */
```

Avoid:

```kotlin
// 이 함수는 장소를 검색한다.
// placeName 필드이다.
```

## Maintenance

When changing backend logic, update nearby comments if the flow, boundary, error behavior, cache shape, or external mapping changes. Remove stale comments rather than preserving outdated explanations.
