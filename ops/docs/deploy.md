# Deployment Notes

This monorepo uses an app-and-ops split inside one repository.

Current target topology:
- `frontend/` -> Vercel (`https://tri-be.app`)
- `backend/` -> Kubernetes via `ops/helm/tribe-api` (`https://api.tri-be.app`)

Required backend prerequisites before production rollout:
- add a backend `Dockerfile`
- expose actuator health and Prometheus endpoints on a dedicated management port
- allow actuator health and Prometheus routes through Spring Security
- configure forwarded headers for reverse proxy operation

Recommended ingress behavior:
- host: `api.tri-be.app`
- sticky session enabled for `/ws`
- extended read/send timeouts for websocket traffic
- TLS termination can stay outside the cluster when Cloudflare Tunnel or another external proxy handles HTTPS

Required runtime configuration:
- backend `APP_URL` must point to the frontend canonical origin: `https://tri-be.app`
- frontend `VITE_API_BASE_URL` must point to `https://api.tri-be.app/api/v1`
- frontend `VITE_BACKEND_ORIGIN` must point to `https://api.tri-be.app`

External platform changes:
- Vercel custom domain: `tri-be.app`
- Cloudflare Tunnel/DNS: route `api.tri-be.app` to the existing ingress/LB target
- Kakao OAuth redirect URI: `https://api.tri-be.app/login/oauth2/code/kakao`

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
