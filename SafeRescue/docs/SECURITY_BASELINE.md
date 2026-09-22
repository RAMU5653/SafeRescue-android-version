# Security Baseline — Phase 1

## Threats considered

- accidental exposure of test credentials
- cleartext network use
- backup leakage of future sensitive data
- secrets accidentally committed to source
- excessive permissions
- UI/business-logic coupling that could make emergency code unsafe later

## Controls in this phase

- Demo credentials are debug-only.
- Release disables the demo login.
- Cleartext traffic is disabled.
- Application backup is disabled.
- No emergency permissions are requested yet.
- No sensitive data is persisted.
- Future emergency logic is not placed in Composables.

## Not implemented yet

Authentication, OTP, Keystore, Room, encrypted evidence, network security configuration, emergency foreground service, GPS, CameraX, microphone, local AI, offline queue, backend authorization and hardware transports are later phases by design.
