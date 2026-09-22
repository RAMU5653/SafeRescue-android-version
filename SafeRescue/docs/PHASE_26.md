# SafeRescue — Phase 26: Release Packaging & Production Readiness

## Purpose
Prepare the SafeRescue Android project for a controlled production release without embedding signing secrets or weakening the emergency/security architecture.

## Release configuration
- applicationId: `com.saferescue.app`
- targetSdk: 35
- minSdk: 26
- versionCode: 18
- versionName: 1.0.0
- Release demo login: disabled
- Release backend URL: empty until a real production HTTPS endpoint is provisioned
- Cleartext traffic: disabled
- Emergency service: not exported
- Production signing: external, operator-controlled

## Signing policy
A production APK/AAB must be signed with an organization-controlled keystore. The keystore, passwords, aliases, and signing properties must never be committed to source control or packaged in the project ZIP.

Recommended CI/environment variables or a local ignored `keystore.properties` file may supply signing values. A template is provided as `keystore.properties.example`.

This phase intentionally does **not** generate or embed a production private key.

## Release preflight
Run:

```bash
bash tools/release_preflight.sh
```

The script checks release configuration, demo-login disablement, cleartext policy, exported emergency service, HTTPS-only backend configuration, and obvious credential patterns.

## Build
A release artifact can be produced only in an Android/Gradle environment with the required SDK and dependencies available. This workspace does not claim a compiled APK/AAB unless the build is actually executed successfully.

Typical commands once Gradle tooling is available:

```bash
./gradlew clean assembleRelease
./gradlew bundleRelease
```

## Artifact verification
After building:

```bash
sha256sum app/build/outputs/apk/release/*.apk
sha256sum app/build/outputs/bundle/release/*.aab
```

The checksum should be recorded with the exact release artifact.

## Security release gates
Before public distribution:
1. Configure a real HTTPS backend.
2. Verify server-side authentication/authorization and IDOR protections.
3. Configure real OTP delivery and rate limits.
4. Perform device/emulator regression tests.
5. Test SOS with network disabled.
6. Test SOS with location/camera/microphone unavailable.
7. Test cancellation and evidence deletion.
8. Test offline queue and duplicate prevention.
9. Test TalkBack and large-font UI.
10. Perform APK/AAB signing and signature verification.
11. Perform privacy/data-retention review.
12. Perform final security review of third-party dependencies.

## Known limitation
This repository is release-ready in structure, but it is **not a claim of production certification**. A real backend, production signing identity, device testing, and final operational security review remain required.
