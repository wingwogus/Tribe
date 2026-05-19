# Tribe Agent Map

This file is a routing map for coding-time harness engineering. Keep it short and use the linked docs as the durable source of detail.

## Codex Discovery Contract

Codex builds project guidance from the repository root down to the current working directory. A nested `AGENTS.md` is loaded automatically only when Codex starts in that directory or below it.

If Codex starts at the repository root, it does not automatically load `frontend/AGENTS.md`, `backend/AGENTS.md`, or `ops/AGENTS.md`. Before touching those areas from a root-launched session, explicitly read the relevant nested file.

Do not create repository `AGENTS.override.md` files by default. Reserve `AGENTS.override.md` for temporary or exceptional stronger overrides because it replaces `AGENTS.md` for the same directory.

## Canonical Sources

- Execution plans live in `.omx/plans/`.
- Generated context, interviews, specs, state, and logs stay under `.omx/`.
- Shared contracts live in `docs/contracts/`.
- Repeatable verification and release procedures live in `docs/runbooks/`.
- Existing README files remain product/setup overviews; do not duplicate them into AGENTS files.

## Task Routing

Read only what is relevant for the task:

- Frontend work: `frontend/AGENTS.md`.
- Backend work: `backend/AGENTS.md`.
- Ops, deploy, secrets, or environment work: `ops/AGENTS.md`.
- API shape or realtime contract changes: `docs/contracts/api.md`.
- Error envelope, code, validation detail, or exception handling changes: `docs/contracts/errors.md`.
- Fixture, seed, or local test data changes: `docs/contracts/fixtures.md`.
- Local test/build/smoke work: `docs/runbooks/local-harness.md`.
- Branch, commit, or PR workflow questions: `docs/runbooks/git-flow.md`.
- Deployment verification: `docs/runbooks/deployment-smoke.md`.

## Harness Rule

Harness engineering means contract, fixture, scenario, and verification come before feature expansion.

A feature is not done unless at least one requirement is locked by an automated check or a documented manual smoke check. If verification is skipped, report the reason and residual risk.

## Git Rules

- Commit subjects must use Conventional Commits: `type(scope): 제목`.
- Allowed types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- Full branch and PR policy lives in `docs/runbooks/git-flow.md`.
- GitHub PRs must use `.github/pull_request_template.md` and explicitly fill verification, skipped checks, residual risk, and contract/runbook changes.
- For decision-heavy commits, keep the Conventional Commits subject and add an explanatory body or git-native trailers when they materially capture constraints, rejected alternatives, test evidence, or residual risk.

## Boundaries

- Prefer small, reversible changes.
- No new production dependencies without explicit user request.
- Keep root guidance concise; move durable details to `docs/`.
- Keep frontend, backend, and ops rules separate.
