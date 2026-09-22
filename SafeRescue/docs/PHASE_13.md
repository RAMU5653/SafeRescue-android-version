# Phase 13 — Backend Sync & Secure REST Transport

## Scope
Adds an authenticated HTTPS transport and connects the Phase 12 encrypted offline queue to WorkManager.

## Contract
- `POST /v1/incidents` with JSON incident metadata.
- `PUT /v1/incidents/{incidentId}/evidence/{filename}` with the already-encrypted evidence blob.
- `Authorization: Bearer <session token>`.
- `Idempotency-Key` is required on incident and each evidence upload.
- Server must authenticate the token, authorize incident ownership, enforce object size/type/path rules, and treat idempotency keys atomically.

## Security
- HTTPS only; no cleartext fallback and no custom trust manager.
- No API key, password, or backend secret is compiled into the client.
- Backend URL is a build-time configuration placeholder and is empty by default.
- Session token is read from the existing Keystore-backed session store and zeroed from the temporary byte array after client construction.
- Encrypted evidence remains encrypted during transport; no decryption is performed by the sync worker.
- 401/403 remain retryable so an eventual refreshed authenticated session can recover them; they are not reported as successful uploads.
- 409 is treated as an idempotent duplicate acknowledgement.
- 5xx/timeout/rate-limit/network failures use the existing durable retry/backoff queue.

## Limitations
A production backend is not bundled or fabricated in this phase. Release authentication still needs a real token-issuing backend and server-side authorization. The empty endpoint means sync remains queued until configured.
