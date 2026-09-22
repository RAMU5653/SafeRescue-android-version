# SafeRescue Phase 10 — Encrypted Evidence & Integrity

## Scope
Phase 10 hardens temporary emergency evidence storage without adding backend upload or retention policy yet.

## Implemented
- AES-256-GCM encryption using an Android Keystore key.
- Unique 12-byte random IV per evidence file.
- Encrypted files live under app-private `files/evidence_secure`.
- Camera JPEG is encrypted immediately after CameraX writes it; plaintext temporary file is best-effort securely deleted.
- Victim voice PCM is encrypted while being written through a `CipherOutputStream`.
- SHA-256 is calculated over the final encrypted artifact for integrity/reference.
- No public media storage and no evidence contents in logs.

## Security model
Manual SOS remains independent from evidence encryption. If encryption, camera, voice, or storage fails, the emergency state is not cancelled.

The hash is an integrity fingerprint, not a signature or proof of authenticity by itself. Key management, durable metadata, cancellation-wide deletion, upload queueing and server-side verification are later phases.

## Limitations
- Current key is device/app-local and intentionally not exported.
- No backend upload in this phase.
- No durable Room evidence index yet.
- CameraX capture still needs a temporary plaintext file because its file-output API requires a file; it is encrypted immediately and the plaintext is deleted best-effort.
