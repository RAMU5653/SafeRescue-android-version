# SafeRescue — Phase 7: CameraX Evidence Capture

## Scope
Phase 7 adds a real CameraX camera surface for an already-active emergency. It supports switching between the front and back camera where the device exposes both selectors and captures JPEG photos into app-private cache storage.

## Safety behavior
- Camera permission is requested contextually, only when the user opens the camera feature.
- Camera denial, unavailability, or capture failure does not cancel or block the SOS state machine.
- The emergency flow remains independent of CameraX.
- Captures are temporary and are not uploaded in Phase 7.
- Phase 10 will add the production encrypted evidence lifecycle, hashes, retention and cancellation deletion guarantees.

## Android/security notes
- `android.permission.CAMERA` is declared.
- CameraX binds to the Activity lifecycle; this avoids pretending that arbitrary background camera recording is universally available to third-party Android apps.
- Camera output is written under `cacheDir/evidence_pending`, never public/shared storage.
- No camera frames are logged.
- No cloud service or API secret is involved.

## UI
- Emergency dashboard exposes Evidence / Camera only during an active emergency.
- Front/back switch control is available.
- Capture feedback is shown locally.
- Accessibility content descriptions are provided for camera controls.

## Test plan
1. Build/install the debug app.
2. Sign in with the debug account `admin` / `admin`.
3. Hold SOS for 5 seconds and let the emergency become active.
4. Open Evidence / Camera.
5. Grant camera permission.
6. Verify preview appears and capture a photo.
7. Switch front/back and verify the selector changes when supported.
8. Deny camera permission and verify the emergency remains active.
9. Confirm captured file is under app-private cache only.

## Known limitation
Phase 7 intentionally implements photo capture rather than claiming continuous background video recording. CameraX requires an appropriate lifecycle and Android's camera/background restrictions vary by OS/device. Continuous evidence recording and encrypted evidence handling are addressed in later phases.
