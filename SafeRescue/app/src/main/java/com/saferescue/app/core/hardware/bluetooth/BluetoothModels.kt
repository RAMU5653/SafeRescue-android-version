package com.saferescue.app.core.hardware.bluetooth

/** Optional BLE link to an ESP32 safety accessory. Phone SOS never depends on this link. */
data class BluetoothEmergencyPacket(
    val messageType: BluetoothMessageType,
    val deviceId: String,
    val timestampMillis: Long,
    val emergencyState: BluetoothEmergencyState,
    val batteryPercent: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val nonce: String,
)

enum class BluetoothMessageType { SOS, HEARTBEAT, CANCEL, CONFIRM }
enum class BluetoothEmergencyState { IDLE, ACTIVE, CANCELLED, CONFIRMED }
enum class BluetoothStatus { DISABLED, NOT_CONFIGURED, CONNECTING, CONNECTED, DEGRADED, ERROR }

sealed interface BluetoothResult {
    data object Sent : BluetoothResult
    data object NotConfigured : BluetoothResult
    data class Failed(val reason: String) : BluetoothResult
}
