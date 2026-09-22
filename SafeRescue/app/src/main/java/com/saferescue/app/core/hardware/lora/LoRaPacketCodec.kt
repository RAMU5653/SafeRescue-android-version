package com.saferescue.app.core.hardware.lora

import java.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Versioned, deterministic wire format. The shared secret is injected at runtime;
 * this class never contains a production credential.
 *
 * Format:
 * SRL1|type|device|timestamp|state|battery|lat|lon|accuracy|nonce|hmacBase64
 */
object LoRaPacketCodec {
    private const val VERSION = "SRL1"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val MAX_PACKET_BYTES = 512

    fun encode(packet: LoRaEmergencyPacket, sharedSecret: ByteArray): ByteArray {
        require(sharedSecret.isNotEmpty()) { "LoRa shared secret must not be empty" }
        validate(packet)
        val unsigned = listOf(
            VERSION,
            packet.messageType.name,
            packet.deviceId,
            packet.timestampMillis.toString(),
            packet.emergencyState.name,
            packet.batteryPercent?.toString().orEmpty(),
            packet.latitude?.toString().orEmpty(),
            packet.longitude?.toString().orEmpty(),
            packet.accuracyMeters?.toString().orEmpty(),
            packet.nonce,
        ).joinToString("|")
        val mac = hmac(unsigned.toByteArray(StandardCharsets.UTF_8), sharedSecret)
        val encoded = unsigned + "|" + Base64.getEncoder().encodeToString(mac)
        val bytes = encoded.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_PACKET_BYTES) { "LoRa packet exceeds $MAX_PACKET_BYTES bytes" }
        return bytes
    }

    fun decode(bytes: ByteArray, sharedSecret: ByteArray): LoRaEmergencyPacket {
        require(sharedSecret.isNotEmpty()) { "LoRa shared secret must not be empty" }
        require(bytes.size <= MAX_PACKET_BYTES) { "LoRa packet exceeds $MAX_PACKET_BYTES bytes" }
        val text = bytes.toString(StandardCharsets.UTF_8)
        val fields = text.split('|')
        require(fields.size == 11 && fields[0] == VERSION) { "Invalid LoRa packet" }
        val unsigned = fields.dropLast(1).joinToString("|")
        val receivedMac = Base64.getDecoder().decode(fields[10])
        val expectedMac = hmac(unsigned.toByteArray(StandardCharsets.UTF_8), sharedSecret)
        require(java.security.MessageDigest.isEqual(receivedMac, expectedMac)) { "Invalid LoRa authentication tag" }
        val packet = LoRaEmergencyPacket(
            messageType = LoRaMessageType.valueOf(fields[1]),
            deviceId = fields[2],
            timestampMillis = fields[3].toLong(),
            emergencyState = LoRaEmergencyState.valueOf(fields[4]),
            batteryPercent = fields[5].takeIf { it.isNotEmpty() }?.toInt(),
            latitude = fields[6].takeIf { it.isNotEmpty() }?.toDouble(),
            longitude = fields[7].takeIf { it.isNotEmpty() }?.toDouble(),
            accuracyMeters = fields[8].takeIf { it.isNotEmpty() }?.toFloat(),
            nonce = fields[9],
        )
        validate(packet)
        return packet
    }

    private fun hmac(data: ByteArray, secret: ByteArray): ByteArray =
        Mac.getInstance(HMAC_ALGORITHM).apply { init(SecretKeySpec(secret, HMAC_ALGORITHM)) }.doFinal(data)

    private fun validate(packet: LoRaEmergencyPacket) {
        require(packet.deviceId.length in 1..64 && packet.deviceId.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        require(packet.nonce.length in 8..64 && packet.nonce.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        require(packet.batteryPercent == null || packet.batteryPercent in 0..100)
        require(packet.latitude == null || packet.latitude in -90.0..90.0)
        require(packet.longitude == null || packet.longitude in -180.0..180.0)
        require(packet.accuracyMeters == null || packet.accuracyMeters >= 0f)
    }
}
