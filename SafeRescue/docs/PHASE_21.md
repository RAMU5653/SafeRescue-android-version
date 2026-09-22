# SafeRescue Phase 21 — Accessibility

## Purpose
Make the existing SafeRescue emergency and dashboard experience usable with TalkBack, accessibility actions, larger text, and non-touch input without weakening emergency safety.

## Architecture
Compose semantics are added at the UI boundary. The emergency service remains authoritative for emergency state. An accessibility SOS action starts a five-second safety delay in the foreground service, preserving an intentional activation window.

## Data flow
TalkBack/custom action → EmergencyUiViewModel → EmergencyForegroundService → 5-second delay → existing SOS path.

## Permissions
No new Android permissions.

## Security
Accessibility must not expose private evidence, credentials, location coordinates, or secrets. The accessible SOS path does not bypass the five-second safety delay. Manual SOS remains authoritative.

## Failure modes
If the service cannot start, no emergency is falsely reported. If accessibility services are unavailable, normal touch controls remain unchanged.

## Testing
- TalkBack focus/order
- Custom SOS action
- Five-second accessibility SOS delay
- Cancel pending accessibility SOS
- Larger font/display size
- Minimum touch targets
- Camera controls and headings
- Existing emergency regression tests

## Known limitations
Full device-level TalkBack validation requires a physical/emulator Android environment. The current UI still contains some legacy fixed-size typography that should be reviewed during final UI polish.
