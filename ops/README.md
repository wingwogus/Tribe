# Tribe Ops

This directory holds the deployment and GitOps configuration for Tribe.

Current deployment split:
- `frontend/` deploys to Vercel
- `ops/helm/tribe-api` deploys the backend API to Kubernetes
- `ops/argocd` holds Argo CD `Application` manifests
- `ops/image-updater` holds Argo CD Image Updater manifests
- `ops/secrets` holds secret/config examples; production secrets should live here as `sops`-encrypted manifests

Production domains:
- frontend: `https://tri-be.app`
- backend: `https://api.tri-be.app`

Runtime contract:
- backend `APP_URL` means the frontend canonical origin, not the API host
- frontend `VITE_API_BASE_URL` should be `https://api.tri-be.app/api/v1`
- frontend `VITE_BACKEND_ORIGIN` should be `https://api.tri-be.app`

Recommended flow:
1. Build and push the backend image from the app repo CI.
2. Argo CD syncs `ops/helm/tribe-api`.
3. Image Updater writes the promoted image tag back into `ops/helm/tribe-api/values-prod.yaml`.
4. Prometheus scrapes the backend management port through the generated `ServiceMonitor`.

Notes:
- This chart is backend-only by design because the frontend is intended to run on Vercel.
- The chart assumes the backend will expose actuator health and Prometheus endpoints on a dedicated management port.
- HTTPS can terminate outside the cluster when Cloudflare Tunnel is fronting the ingress.
