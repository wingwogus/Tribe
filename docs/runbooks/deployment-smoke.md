# Deployment Smoke Runbook

This runbook covers post-deploy and release verification for Tribe.

## Target Topology

- Frontend: Vercel at `https://tri-be.app`
- Backend API: Kubernetes via `ops/helm/tribe-api` at `https://api.tri-be.app`
- GitOps: Argo CD manifests under `ops/argocd`
- Image automation: Argo CD Image Updater manifests under `ops/image-updater`

## Required Context

Read these before deployment or smoke work:

- `ops/README.md`
- `ops/docs/deploy.md`
- `ops/secrets/README.md`
- `docs/runbooks/git-flow.md`

## Pre-deploy Checks

- Confirm environment variables are documented with safe placeholders.
- Confirm real secrets are encrypted or excluded from git.
- Confirm backend image tag and Helm values point to the intended release.
- Confirm frontend environment points to `https://api.tri-be.app/api/v1` and `https://api.tri-be.app`.
- Confirm rollback or recovery path is written for deployment-affecting changes.

## Smoke Targets

Run the checks that match the deployed change.

Frontend:

- `https://tri-be.app` loads.
- Main SPA routes deep-link correctly.
- OAuth redirect starts against the backend origin when auth is in scope.

Backend:

- `https://api.tri-be.app` is reachable through the configured ingress or tunnel.
- Actuator health is checked only after the deploy prerequisites in `ops/docs/deploy.md` are implemented for the target environment.
- A representative `/api/v1` endpoint returns the expected response envelope.

Realtime:

- WebSocket endpoint `/ws` is reachable.
- Ingress behavior preserves sticky sessions and extended read/send timeouts where relevant.
- Trip and chat topics continue to update frontend caches after deployment.

## Rollback or Recovery Notes

Deployment changes should state one of:

- previous image tag or Helm values to restore
- Argo CD rollback/sync action
- Vercel rollback target
- manual recovery step and owner

## Completion Report

Every deployment smoke should report:

- environment checked
- command or URL checked
- pass/fail result
- skipped checks and reason
- rollback or recovery note
- known residual risk
