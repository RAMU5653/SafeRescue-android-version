# SafeRescue — Phase 4: Emergency State Machine

## Current phase
Phase 4 only. SOS timing, sensors, AI, GPS, camera, microphone, networking and hardware are intentionally not implemented here.

## Architecture
`UI -> ViewModel/use case -> EmergencyManager -> EmergencyRepository`

`EmergencyManager` is the single owner of lifecycle transitions. UI code must observe state and must not keep emergency booleans.

## States
IDLE, SOS_HOLDING, EMERGENCY_ACTIVE, MONITORING, CANCELLATION_PENDING, CANCELLED, CONFIRMED, EVIDENCE_PRESERVATION, UPLOADING, QUEUED_OFFLINE, SYNCING, COMPLETED, FAILED_RECOVERABLE, FAILED_FATAL.

## Security review
- Manual SOS is not gated by AI confidence or sensor availability.
- Invalid transitions fail closed and do not mutate state.
- Cancellation cannot transition directly to upload/confirmation.
- Confirmed incidents have an explicit evidence-preservation state before upload/queue.
- Thread safety is enforced with synchronized state access.
- No credentials, tokens, media, location or other sensitive data are logged or stored by this phase.
- The repository boundary allows Room to be introduced later without moving business logic into UI.

## Failure independence
Future GPS/camera/voice/AI/Bluetooth/LoRa modules must report their own status; they must not directly terminate the emergency state. The state machine only treats a fatal failure as terminal when explicitly dispatched by the emergency orchestration layer.

## Testing
Unit tests cover manual activation, cancellation, confirmed online path, offline queue path, invalid transitions and cancellation upload blocking.

## Limitations
Phase 4 does not yet activate an SOS from the UI, start a foreground service, run a timer, capture evidence, use sensors, or upload incidents. Those belong to later phases.
