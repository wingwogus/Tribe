# Secrets

Production secrets should not be committed in plaintext.

Recommended workflow:
1. Copy the example manifest that matches the target object.
2. Fill in the real values locally.
3. Encrypt the manifest with `sops`.
4. Commit the encrypted `*.enc.yaml` version into this directory.

This repo is configured to encrypt `ops/secrets/*.enc.yaml` with the local age recipient declared in `.sops.yaml`.

Typical commands:

```bash
sops --encrypt --in-place ops/secrets/tribe-api-secret.enc.yaml
sops --encrypt --in-place ops/secrets/tribe-api-config.enc.yaml
```

Apply manually when needed:

```bash
sops --decrypt ops/secrets/tribe-api-secret.enc.yaml | kubectl apply -n tribe -f -
sops --decrypt ops/secrets/tribe-api-config.enc.yaml | kubectl apply -n tribe -f -
```

Expected runtime object names:
- `tribe-api-secret`
- `tribe-api-config`

Suggested file naming:
- `tribe-api-secret.enc.yaml`
- `tribe-api-config.enc.yaml`

Config conventions:
- `APP_URL` is the frontend canonical origin consumed by backend CORS, OAuth redirects, websocket origin checks, and invite link generation.
- For the current production split, set `APP_URL` to `https://tri-be.app`.
- The backend ingress host is configured separately in Helm and should point to `api.tri-be.app`.
