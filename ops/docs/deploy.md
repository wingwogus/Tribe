# Deployment Notes

This monorepo uses an app-and-ops split inside one repository.

Current target topology:
- `frontend/` -> Vercel (`https://tribe.jaehyuns.com`)
- `backend/` -> Kubernetes via `ops/helm/tribe-api` (`https://api.jaehyuns.com`)

Required backend prerequisites before production rollout:
- add a backend `Dockerfile`
- expose actuator health and Prometheus endpoints on a dedicated management port
- allow actuator health and Prometheus routes through Spring Security
- configure forwarded headers for reverse proxy operation

Recommended ingress behavior:
- host: `api.jaehyuns.com`
- sticky session enabled for `/ws`
- extended read/send timeouts for websocket traffic

Argo CD source path:
- repo: this `Tribe` repository
- path: `ops/helm/tribe-api`

Image Updater write-back path:
- `ops/helm/tribe-api/values.yaml`

Image Updater notes:
- Only the backend image is updater-managed because the frontend deploys on Vercel.
- The updater writes back into this same monorepo, relative to the Argo CD app source path.
- Expected write-back target:
  - `image.repository`
  - `image.tag`
- Expected tag shape:
  - commit SHA tags only
  - `latest` is ignored
- If the registry is private, configure registry credentials for Argo CD Image Updater in `argocd`.
- Git write-back requires a `git-creds` secret in the `argocd` namespace.
