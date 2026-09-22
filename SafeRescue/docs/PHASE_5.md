# SafeRescue Phase 5 — SOS hold, countdown and cancellation

## Implemented
- 5-second continuous SOS hold with visible progress.
- Haptic feedback on successful activation.
- EmergencyForegroundService owns the 120-second countdown and posts a high-priority ongoing notification.
- Activity recreation does not own or stop the countdown.
- 5-second cancellation hold transitions to CANCELLATION_PENDING immediately, then CANCELLED on completion.
- Releasing cancellation early returns to MONITORING.
- Timer expiry transitions to CONFIRMED.
- Manual SOS is not gated by AI, camera, microphone, GPS, network, Bluetooth or LoRa.
- No sensor capture is started in Phase 5; those belong to later phases.

## Android limitation
The foreground service uses Android's `specialUse` foreground-service category because emergency safety monitoring does not map cleanly to the standard location/media types yet. Production distribution must satisfy Android/Play foreground-service policy and provide the appropriate declaration/review.

The service is `START_NOT_STICKY` in this phase. Durable emergency persistence across full process/device death belongs to the later database/evidence/offline phases and must not be claimed as complete yet.
