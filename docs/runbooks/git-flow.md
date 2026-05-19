# Git Flow Runbook

This repository uses a simplified Git Flow model.

## Commit Messages

All commit subjects must use Conventional Commits:

```text
type(scope): 제목
```

Examples:

```text
feat(settlement): 최종 정산 알고리즘 구현
fix(auth): 카카오 로그인 시 간헐적 오류 수정
docs(readme): ERD 다이어그램 업데이트
```

Allowed types:

- `feat`: new feature
- `fix`: bug fix
- `docs`: documentation-only change
- `refactor`: behavior-preserving code restructuring
- `test`: test additions or changes
- `chore`: build, package manager, tooling, or maintenance work

Use a concise Korean title after the scope. Choose the type by the actual committed change.

## Decision-record Compatibility

Use the Conventional Commits subject line even when a commit needs more context.

For decision-heavy changes, add a body and git-native trailers only when they materially capture constraints, rejected alternatives, test evidence, or residual risk.

Example:

```text
docs(harness): 에이전트 문서 하네스 추가

Codex instruction discovery loads nested AGENTS files only when launched from
that directory path, so root guidance stays as a router and detailed contracts
live under docs.

Constraint: Root-launched Codex sessions do not automatically load nested AGENTS files
Rejected: Use AGENTS.override.md everywhere | hides same-directory AGENTS.md
Confidence: high
Scope-risk: narrow
Tested: documentation existence checks
Not-tested: live Codex CLI instruction-source summary
```

## Branch Model

- `main`: final deployable stable branch.
- `dev`: central development branch.
- `feat/{feature-name}`: feature branch for individual work, created from `dev`.

Examples:

```text
feat/settlement-algorithm
feat/community-crud
feat/coding-harness-docs
```

## Development Flow

1. Start from `dev`.
2. Create a feature branch: `feat/{feature-name}`.
3. Complete implementation and verification on the feature branch.
4. Open a Pull Request back to `dev`.
5. At least two reviewers must review and approve before merge.
6. Each reviewer should leave at least one question after reading the code.
7. Release by merging `dev` into `main`.

## Harness Requirement

PR descriptions or final agent reports should include:

- branch name
- summary of changed files
- verification evidence
- skipped checks and residual risk
- contract or runbook updates, if applicable
