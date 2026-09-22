package com.saferescue.app.core.hardware.lora

/**
 * Optional adapter for publishing compact emergency state to a gateway.
 * Callers must treat failure as non-fatal: the phone emergency remains authoritative.
 */
class LoRaEmergencyBridge(private val transport: LoRaGatewayTransport) {
    suspend fun publish(packet: LoRaEmergencyPacket): LoRaGatewayResult =
        runCatching { transport.send(packet) }
            .getOrElse { LoRaGatewayResult.Failed("Gateway transport failed") }
}
