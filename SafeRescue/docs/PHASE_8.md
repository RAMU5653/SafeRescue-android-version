# SafeRescue Phase 8 — Victim Voice Pipeline

## Scope
Phase 8 adds a real Android microphone capture pipeline for the victim's voice during an active manual SOS. It does not implement AI distress classification; Phase 9 owns risk scoring and later Phase 21 owns real on-device AI.

## Implementation
- `core/voice`: status, metrics, repository abstraction.
- `AndroidVoiceRepository`: 16 kHz mono PCM16 capture using `AudioRecord`.
- Temporary PCM is written only to app-private cache under `voice_pending/`.
- Live metrics expose timestamp, duration, RMS dB, peak amplitude and byte count.
- Emergency service owns start/stop so cancellation and confirmation stop capture.
- UI requests microphone permission only after manual SOS activation.

## Security/privacy
- `RECORD_AUDIO` and microphone FGS permissions are declared.
- No microphone data is sent to a server in this phase.
- No raw voice is logged.
- No public/shared storage is used.
- Voice is explicitly the victim's voice pipeline; this phase does not label a person, crime, or distress.
- Phase 10 must add encryption/integrity and retention/deletion guarantees before production evidence handling.

## Android limitation
A foreground service can only use the microphone under Android's current permission/foreground-service rules. If permission is denied or the microphone is unavailable, the SOS continues independently.

## Test plan
1. Trigger manual SOS.
2. Grant/deny microphone permission and confirm emergency continues either way.
3. With permission granted, confirm status becomes LISTENING and metrics update.
4. Hold cancel for 5 seconds and confirm voice capture stops.
5. Let countdown confirm and confirm voice capture stops.
