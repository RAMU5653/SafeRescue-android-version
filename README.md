# SafeRescue

SafeRescue is a local-first Android safety platform being developed incrementally from the master specification.

## Current phase
**Phase 17 — LoRa + Raspberry Pi Gateway**

Implemented in this phase:
- Phase 2 authentication remains intact
- authenticated dashboard with Home / Safety / Evidence / Me navigation
- SafeRescue status card and capability cards
- explicit non-active states for future emergency features
- profile/session surface and logout
- dashboard navigation state isolated in HomeViewModel
- restrained accessible motion and security-aware presentation

Not yet implemented by design:
- SOS
- emergency state machine
- GPS
- CameraX
- victim voice
- local AI
- risk engine
- evidence encryption lifecycle
- offline incident queue
- backend synchronization
- trusted contacts
- Bluetooth/LoRa hardware

Those belong to later phases and will not be faked as complete.

## Build
Open the `SafeRescue` directory in Android Studio and sync Gradle. Use the **debug** variant for the development test account.

## Development test account
`admin / admin`

This account is a development fixture only. It must not be used as production authentication.

## Skills
The project applies the requested Impeccable, Taste, Motion Skills and Anthropic-Cybersecurity-Skills as engineering references for design, motion, security and testing.


## Phase 6
GPS/location tracking is implemented as an emergency-only capability using Fused Location Provider. Permission is requested contextually after manual SOS activation. Location failure never blocks or cancels a manual emergency. See docs/PHASE_6.md.

## Phase 7
CameraX evidence capture has been added. The camera is optional, front/back selection is supported where available, and temporary captures stay in app-private cache until the encrypted evidence phase.

## Phase 8
Victim voice pipeline added: microphone permission after SOS, Android AudioRecord 16 kHz mono PCM capture, app-private temporary storage, live signal metrics, and emergency-safe failure handling. No AI distress classification is claimed in this phase.


## Phase 9
Local deterministic 0–100 heuristic risk engine with auditable factors and severity thresholds. Risk never overrides manual SOS.

## Phase 10
Adds Android Keystore AES-256-GCM encrypted evidence storage, SHA-256 integrity fingerprints, encrypted voice capture, and immediate encryption of CameraX captures. Evidence remains app-private; backend upload and durable metadata are later phases.

## Phase 12 — Offline Encrypted Queue
- Durable AES-256-GCM encrypted queue in app-private storage.
- WorkManager network constraint and retry scheduling.
- Confirmed incidents are queued with stable idempotency keys; Phase 13 supplies the real backend transport.

## Phase 13 — Backend Sync
Authenticated HTTPS transport and WorkManager queue draining are implemented. The backend endpoint is intentionally empty by default; configure a real HTTPS backend in the build before expecting sync. No production backend or fake police endpoint is included.

## Phase 14 — Trusted Contacts + Notifications
- Up to 3 trusted contacts with encrypted local storage, add/edit/remove, explicit local verification state, and notification test flow.
- Android 13+ notification permission and a dedicated SafeRescue alerts channel.
- External SMS/push/police delivery is not fabricated; it remains a secure backend integration step.
- See `docs/PHASE_14.md`.

## Phase 15
Added local PDF incident report generation from incident metadata, risk summary, and timeline. Reports remain in app-private storage and do not embed raw camera/voice evidence.


## Phase 16
Adds user-triggered live safety guidance: nearby mapped police/hospital/fire-station/public places from OpenStreetMap Overpass and current weather from Open-Meteo. External coordinate sharing is disclosed in the UI; the feature never controls the SOS state machine.


## Phase 17 — LoRa + Raspberry Pi Gateway
Adds a modular optional LoRa hardware path for compact SOS/status packets: ESP32 + LoRa → Raspberry Pi gateway → HTTPS backend. Android defines the gateway boundary and authenticated SRL1 packet codec without claiming a live LoRa connection. HMAC authentication, timestamp freshness, nonce replay protection, bounded parsing and HTTPS-only forwarding are included. Hardware failure never blocks the phone emergency. See `docs/PHASE_17.md`.

## Phase 18 — Real Local AI Model Runtime
Adds a TensorFlow Lite local-model runtime boundary and 64-feature voice input adapter.
The app never fabricates an AI result: without a validated `voice_distress.tflite` asset,
status is explicitly `NOT_INSTALLED`. Model execution is local-only and manual SOS is
never dependent on AI. See `docs/PHASE_18.md`.


## Phase 19 — ESP32 Bluetooth (BLE)
Optional BLE transport for a paired/configured ESP32 accessory. Uses bounded HMAC-authenticated emergency packets and never blocks the phone SOS path. Bluetooth is disabled/not configured until real hardware is provisioned.


## Phase 20
Performance + battery optimization: battery/power-save/thermal-aware throttling of optional voice metrics and risk recalculation while preserving continuous evidence capture, 10-second emergency GPS baseline, and the one-second SOS countdown. See `docs/PHASE_20.md`.


## Phase 21 — Accessibility
TalkBack semantics, headings, emergency accessibility actions, accessible camera controls, and safer non-touch SOS activation with a five-second delay.

## Phase 22 — Security Testing & Hardening
- Security regression tests for transport, LoRa/BLE integrity and unsafe configuration.
- Static `tools/security_audit.sh` checks for cleartext transport, exported emergency service, TLS bypass patterns, secret patterns and release demo-login separation.
- Backend client hardened for HTTPS-only transport, safe idempotency keys and fatal 401/403 handling.
- See `docs/PHASE_22.md`.

## Phase 23 — Performance & Battery Optimization (Deepening Pass)
- Adds a bounded 2-second performance snapshot cache using monotonic elapsed time.
- Reduces repeated battery/power/thermal framework queries from high-frequency voice processing.
- Adds broader performance-policy regression tests.
- Optional metric/risk cadence may throttle; manual SOS, countdown, evidence capture and emergency reliability are not throttled.
- See `docs/PHASE_23.md`.

Phase 24 appended to README.
Phase 24: Accessibility completion — larger emergency controls, progress semantics, headings, and live signal announcements.

## Phase 25
Final UI design-system polish: centralized Material 3 colors, typography and shape tokens. Emergency behavior and security architecture are unchanged.

## Phase 26 — Release Packaging & Production Readiness

Release configuration is versioned as **1.0.0 / versionCode 18**. Production signing is intentionally external and no private key is stored in this repository. Run `bash tools/release_preflight.sh` before a real release build. See `docs/PHASE_26.md` for signing, artifact verification, and production security gates.

- Lock-screen emergency access: Android-supported notification action opens a secure lock-screen SOS surface; it never unlocks the device.
images of safeRescue:
<img width="752" height="846" alt="Screenshot 2026-09-22 132607" src="https://github.com/user-attachments/assets/fb0d21d6-24d8-4bd7-b2c1-cdbaffe465cb" />
<img width="597" height="838" alt="Screenshot 2026-09-22 132824" src="https://github.com/user-attachments/assets/497998d6-b706-4f13-accb-63f18c2addf6" />
<img width="547" height="761" alt="Screenshot 2026-09-22 132705" src="https://github.com/user-attachments/assets/544f58c4-2872-41a4-a2ba-3a0ce780cee8" />



