# SafeRescue Phase 22 — Security Testing & Hardening

## Purpose
Phase 22 is a security regression pass over the Phase 21 accessibility build. It adds repeatable security tests and hardens the backend transport without weakening the emergency path.

## Threat areas tested
- Cleartext network transport and TLS downgrade attempts
- Embedded backend credentials/query/fragment misuse
- Bearer-token transport boundary
- Idempotency-key validation
- 401/403 retry behavior (must not loop indefinitely)
- LoRa HMAC authentication and tamper rejection
- BLE bounded authenticated payload generation
- Exported-component baseline
- Release/demo-login separation
- Hard-coded production secret pattern scan

## Hardening changes
1. `SecureBackendClient` rejects non-HTTPS URLs.
2. Backend base URLs containing URL credentials, query parameters, or fragments are rejected.
3. Idempotency keys must be 16–128 characters from a restricted safe character set.
4. HTTP 401/403 responses are treated as fatal authentication/authorization failures instead of retryable network failures.
5. Existing HTTPS-only, no-custom-trust-manager behavior remains intact.

## Automated checks
- `tools/security_audit.sh` performs static security assertions.
- `SecurityRegressionTest.kt` covers transport configuration, idempotency validation, BLE bounds/authentication marker, and LoRa tamper rejection.

## Manual/device tests required before release
- Run unit tests with the project's Android/Gradle toolchain.
- Run on a physical Android device with TalkBack, screen-off emergency flow, permission denial, offline mode, and process recreation.
- Proxy a staging build through an approved TLS inspection environment and verify certificate validation is not bypassed.
- Verify backend authorization/IDOR controls server-side for incident/evidence ownership.
- Verify upload size/type/content validation server-side.
- Verify OTP rate limiting and account enumeration protections on the production auth service.
- Verify real BLE/LoRa provisioning and key rotation on hardware.

## Findings / limitations
- Production backend is not configured in this artifact; server-side controls cannot be fully penetration-tested here.
- No certificate pinning is added because backend endpoint and operational key rotation policy are not yet finalized.
- Debug `admin/admin` exists only in the debug build configuration; release explicitly disables demo login. It must never be shipped as a release APK.
- Emergency state remains process-local/in-memory and should be moved to durable Room state in the appropriate persistence phase.
- Full Gradle compilation/device execution was not available in the build environment, so this phase does not claim an APK build.

## Security posture
**Improved / regression-tested, not production-certified.**
