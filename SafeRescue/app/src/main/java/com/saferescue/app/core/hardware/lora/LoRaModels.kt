package com.saferescue.app.core.hardware.lora

/** Compact emergency message exchanged between an ESP32 LoRa node and a gateway. */
data class LoRaEmergencyPacket(
    val messageType: LoRaMessageType,
    val deviceId: String,
    val timestampMillis: Long,
    val emergencyState: LoRaEmergencyState,
    val batteryPercent: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val nonce: String,
)

enum class LoRaMessageType { SOS, HEARTBEAT, CANCEL, CONFIRM }

enum class LoRaEmergencyState { IDLE, ACTIVE, CANCELLED, CONFIRMED }

enum class LoRaGatewayStatus { DISABLED, NOT_CONFIGURED, CONNECTED, DEGRADED, ERROR }

sealed interface LoRaGatewayResult {
    data object Sent : LoRaGatewayResult
    data object NotConfigured : LoRaGatewayResult
    data class Failed(val reason: String) : LoRaGatewayResult
}
