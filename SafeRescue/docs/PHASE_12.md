# SafeRescue Phase 12 — Offline Encrypted Queue

## Scope
Phase 12 adds a durable, encrypted offline queue for confirmed incident sync intents. The queue is intentionally independent of the backend; Phase 13 provides the authenticated transport.

## Implementation
- `EncryptedOfflineQueue`: app-private `filesDir/offline_queue`, one AES-256-GCM encrypted record per queue item.
- Android Keystore owns the queue encryption key; each record receives a fresh random 12-byte GCM IV.
- Queue payloads contain an incident ID, idempotency key, timestamps, retry metadata and encrypted-evidence file references.
- Queue is bounded to 100 records and payloads to 256 KiB to avoid uncontrolled storage use.
- Atomic temp-file write + rename reduces partial-record corruption.
- `OfflineSyncWorker` uses WorkManager and a `CONNECTED` network constraint. It deliberately returns retry until Phase 13 supplies a real transport; it never claims an upload succeeded.
- Confirmed emergencies create a durable queue item and schedule the worker.
- Exponential retry metadata is capped; duplicate prevention is based on the stable idempotency key carried by each item.

## Security
- No plaintext queue database/file is used.
- No secrets, passwords or tokens are queued.
- Evidence is referenced by app-private encrypted file path; raw evidence is not copied into the queue payload.
- Cancellation does not create a sync item.
- SOS remains independent of WorkManager, network availability and queue state.

## Limitation
Actual network upload, server acknowledgement, idempotent server-side processing and online/offline reconciliation are Phase 13 responsibilities.
