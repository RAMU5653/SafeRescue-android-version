package com.saferescue.app.core.hardware.lora

/**
 * Android-side boundary for the optional LoRa/Raspberry Pi path.
 * The phone does not require LoRa hardware for SOS operation.
 */
interface LoRaGatewayTransport {
    suspend fun send(packet: LoRaEmergencyPacket): LoRaGatewayResult
    fun status(): LoRaGatewayStatus
}

/** Explicitly non-production implementation used until a gateway transport is configured. */
class NotConfiguredLoRaGatewayTransport : LoRaGatewayTransport {
    override suspend fun send(packet: LoRaEmergencyPacket): LoRaGatewayResult = LoRaGatewayResult.NotConfigured
    override fun status(): LoRaGatewayStatus = LoRaGatewayStatus.NOT_CONFIGURED
}
