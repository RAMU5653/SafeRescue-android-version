# Phase 19 — ESP32 Bluetooth (BLE) Hardware Boundary

## Scope
Adds an optional Bluetooth Low Energy transport for a paired/configured ESP32 safety accessory.

## Architecture
Phone emergency state → BluetoothTransport → BLE GATT → ESP32.

The Android emergency flow does not wait for Bluetooth, scanning, pairing, GPS, camera, microphone, AI, network, or LoRa.

## Protocol
- Versioned bounded payload (maximum 512 bytes).
- SOS, HEARTBEAT, CANCEL, CONFIRM message types.
- Device ID, timestamp, state, optional battery/GPS and nonce.
- HMAC-SHA256 application authentication using a runtime-provided key.
- Raw audio/video is never transported over BLE.

## Android security
- `BLUETOOTH_SCAN` uses `neverForLocation`.
- `BLUETOOTH_CONNECT` is required for a configured GATT link.
- No hardcoded BLE secret.
- No plaintext secret logging.
- Transport catches permission/write failures and reports a non-fatal result.

## Hardware
The ESP32 firmware and board-specific provisioning are deployment work. UUIDs are reserved for the SafeRescue BLE profile; the app does not claim hardware is connected until a real GATT connection exists.
