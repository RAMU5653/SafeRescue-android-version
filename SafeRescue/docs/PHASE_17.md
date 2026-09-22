# Phase 17 — LoRa + Raspberry Pi Gateway

## Purpose
Add a modular, security-aware fallback path for compact SOS/status packets:
ESP32 + LoRa radio → Raspberry Pi gateway → HTTPS SafeRescue backend.

The phone SOS remains authoritative and works without LoRa, ESP32, Raspberry Pi,
or network access.

## Architecture
```text
Android SafeRescue                 Optional hardware path
      |                                      |
      | core emergency                       v
      |                              ESP32 + LoRa radio
      |                                      |
      |                                      v
      |                              Raspberry Pi gateway
      |                                      |
      |                              HTTPS + auth token
      |                                      v
      +----------------------------> SafeRescue backend
```

The Pi receives compact packets rather than media. Video, audio and reports are
not sent over LoRa.

## Wire protocol
`SRL1|type|deviceId|timestamp|state|battery|lat|lon|accuracy|nonce|HMAC-SHA256`

Supported message types: `SOS`, `HEARTBEAT`, `CANCEL`, `CONFIRM`.

Fields are bounded and validated. The HMAC covers every field except the HMAC
itself. The shared secret is injected at runtime and is never stored in source.

## Anti-replay controls
- authenticated packets with HMAC-SHA256
- timestamp skew limit of 5 minutes
- per-device nonce replay cache
- bounded packet size (512 bytes)
- strict device/nonce character validation

A production deployment should additionally use per-device key rotation and a
server-side durable replay/idempotency store.

## Raspberry Pi gateway
`gateway/gateway.py` is a reference implementation. It reads newline-framed
packets from an ESP32 serial connection, validates them, rejects replayed or
stale packets, and optionally forwards a compact JSON event to an HTTPS backend.

LoRa/SPI driver details are intentionally outside the gateway process because
the radio can be physically attached to an ESP32 or another supported adapter.

## Android boundary
`core/hardware/lora/` contains:
- `LoRaModels.kt`
- `LoRaPacketCodec.kt`
- `LoRaGatewayTransport.kt`
- `LoRaEmergencyBridge.kt`

The default transport is explicitly `NOT_CONFIGURED`. It does not fake a LoRa
connection. Future hardware integration can implement `LoRaGatewayTransport`
without coupling hardware code to the emergency state machine.

## Security
- no hardcoded LoRa secret
- no hardcoded backend token
- HTTPS required for backend forwarding
- no raw secret/packet logging
- gateway failure is non-fatal to phone SOS
- compact telemetry only; never send camera/voice evidence through LoRa
- backend must authenticate and authorize gateway events server-side
- client/gateway timestamps and risk values must not be trusted as authority

## Failure modes
- LoRa unavailable → phone emergency continues
- ESP32 disconnected → gateway continues waiting
- malformed packet → reject
- invalid HMAC → reject
- stale packet → reject
- replayed nonce → reject
- backend unavailable → gateway reports failure; backend retry/durable delivery
  should be added before production

## Testing
From `gateway/`:
```bash
python3 -m unittest discover -s tests -v
```

Android codec tests should cover valid encoding/decoding, tampering, malformed
fields, bounds and empty secrets when the Android Gradle test toolchain is
available.

## Status
**Phase 17 implementation complete as a modular hardware/gateway architecture.**

**NOT PRODUCTION READY:** actual LoRa radio firmware, Pi hardware wiring,
per-device key provisioning/rotation, durable gateway delivery, and a real
authorized backend hardware endpoint still require deployment-specific work.
