# Phase 2 — Authentication + OTP

## Current phase
Phase 2 only. Emergency, GPS, camera, microphone, AI, backend, Bluetooth and LoRa are intentionally not implemented.

## Implemented
- Username/password login through an `AuthRepository` abstraction.
- Debug-only test account: `admin` / `admin`.
- Registration form: name, username, phone, email, password and confirmation.
- OTP challenge with five-attempt limit and five-minute expiry in the debug adapter.
- Password reset flow using an OTP challenge.
- Passwords are hashed with PBKDF2-HMAC-SHA256 in the local debug adapter rather than stored as plaintext.
- Session username and random session token are encrypted using an AES-GCM key held by Android Keystore.
- Login/session state is separated from Compose UI through `AuthViewModel` and repository interfaces.
- Debug OTP is shown only by the local debug adapter because no SMS provider/backend exists yet.

## Production boundary
The debug adapter is NOT production authentication. A production adapter must send OTPs through a trusted backend/SMS provider, hash passwords on the backend, enforce server-side rate limits, issue short-lived access/refresh tokens, and perform server-side authorization. No backend secrets are embedded in the Android app.

## Security review
- OTP values are not persisted as plaintext; the debug challenge stores a SHA-256 digest and expiry/attempt metadata in memory.
- Password hashes use a per-user random salt and PBKDF2-HMAC-SHA256.
- Login session token is random and encrypted at rest with Android Keystore AES-GCM.
- OTP challenges expire after five minutes and lock after five failed attempts.
- No passwords, tokens or OTPs are written to Logcat.
- Release build continues to disable the fixed `admin/admin` demo fixture by using a separate release configuration path; the debug repository itself is only wired for the current development build.

## Design / skills review
Impeccable principles were applied to hierarchy, accessibility, error states and bounded motion; Taste principles informed spacing, typography and reduction of generic card clutter; Motion guidance is limited to a short, calm auth-screen crossfade so motion never competes with safety actions. The cybersecurity skill library informed least privilege, validation, credential handling, rate limiting and explicit demo-vs-production boundaries. These are engineering references, not runtime security libraries.

## Test plan
1. Debug login with `admin/admin`.
2. Invalid login displays a generic error.
3. Registration rejects weak/invalid fields.
4. Registration generates a debug-only OTP and verifies it.
5. Six incorrect OTP attempts result in rate limiting.
6. OTP expires after five minutes.
7. Password reset OTP changes a registered debug account password.
8. Logout clears the encrypted session.
9. Relaunching the debug app retains the encrypted session state until logout.
