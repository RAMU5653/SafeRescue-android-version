# Phase 6 — GPS / Location

## Implemented
- Fused Location Provider via Google Play services.
- Fine or coarse location permission requested contextually after manual SOS activation.
- Location is not required for emergency activation; denial/failure does not stop SOS.
- Emergency location updates are requested approximately every 10 seconds with a 5-second minimum interval.
- Each location point contains latitude, longitude, timestamp and reported accuracy.
- Location collection is owned by the emergency foreground service, not an Activity.
- Provider/permission failures are surfaced as status signals.
- No public Gallery/Downloads storage is used.

## Privacy/security
- No location permission request during normal dashboard use.
- No background-location permission added.
- Location broadcasts are package-scoped and not exported.
- Coordinates are not logged.
- Accuracy is retained so a fix is not represented as exact.

## Limitation
Room/durable encrypted location timeline is intentionally deferred to later evidence/database phases. Phase 6 keeps the latest point in UI state and consumes live points while the emergency service is alive.

## Test plan
1. Grant location permission, activate SOS, and verify status becomes ACTIVE and a fix can appear.
2. Deny location permission; verify emergency remains active and location reports PERMISSION_REQUIRED.
3. Disable device location providers; verify emergency remains active and location reports UNAVAILABLE.
4. Turn the screen off during an active emergency and verify the foreground-service architecture remains responsible for location where Android/device policy permits.
5. Verify location permissions are not requested before SOS.
