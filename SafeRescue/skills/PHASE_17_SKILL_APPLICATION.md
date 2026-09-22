# Phase 17 Skill Application

## Impeccable / Taste
Keep the hardware path invisible to the core SOS UX unless configured. Expose
status honestly rather than showing a fake connected device.

## Motion
No emergency-critical animation or timing depends on gateway state.

## Cybersecurity skills
Threat model the LoRa boundary: spoofing, tampering, replay, stale packets,
secret leakage, malformed input, and backend impersonation. Use HMAC,
bounded parsing, timestamp checks, nonce replay protection, HTTPS and secret
injection. Treat hardware telemetry as untrusted input.
