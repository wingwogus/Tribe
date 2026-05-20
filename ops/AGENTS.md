# Ops Agent Guide

This file applies to work under `ops/`. If Codex was launched from the repository root, read this file before touching ops files.

## Required Context

- Local harness: `../docs/runbooks/local-harness.md`
- Deployment smoke: `../docs/runbooks/deployment-smoke.md`
- Existing deployment notes: `docs/deploy.md`
- Git workflow: `../docs/runbooks/git-flow.md`

## Current Deployment Split

- `frontend/` deploys to Vercel.
- `ops/helm/tribe-api` deploys the backend API to Kubernetes.
- `ops/argocd` holds Argo CD `Application` manifests.
- `ops/image-updater` holds Argo CD Image Updater manifests.
- `ops/secrets` holds secret/config examples and encrypted production manifests.

Production domains:

- Frontend: `https://tri-be.app`
- Backend: `https://api.tri-be.app`

## Ops Harness Rules

- Do not commit real secrets or machine-specific credentials.
- Environment variable additions must include safe placeholders and documentation.
- Deployment changes must include a smoke check and rollback or recovery note.
- WebSocket/ingress changes must account for sticky sessions and extended timeout behavior where relevant.
- Keep frontend deployment assumptions separate from backend Helm/Kubernetes assumptions.

## Completion Report

Ops changes should report:

- files changed
- smoke checks executed
- rollback or recovery note
- skipped checks and reason
- residual risk
