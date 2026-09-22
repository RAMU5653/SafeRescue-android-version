package com.saferescue.app.core.hardware.bluetooth

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Deterministic, bounded BLE application payload. Never include raw audio/video. */
object BluetoothEmergencyCodec {
    private const val MAX_BYTES = 512
    private const val HMAC_ALGORITHM = "HmacSHA256"

    fun encode(packet: BluetoothEmergencyPacket, key: ByteArray): ByteArray {
        require(packet.deviceId.length in 1..64) { "invalid device id" }
        require(packet.nonce.length in 8..128) { "invalid nonce" }
        require(key.isNotEmpty()) { "BLE key required" }
        val body = listOf(
            "v=1",
            "type=${packet.messageType.name}",
            "device=${packet.deviceId}",
            "ts=${packet.timestampMillis}",
            "state=${packet.emergencyState.name}",
            "battery=${packet.batteryPercent ?: -1}",
            "lat=${packet.latitude ?: ""}",
            "lon=${packet.longitude ?: ""}",
            "nonce=${packet.nonce}"
        ).joinToString("|")
        val signature = hmac(body.toByteArray(StandardCharsets.UTF_8), key)
        val result = "$body|mac=$signature".toByteArray(StandardCharsets.UTF_8)
        require(result.size <= MAX_BYTES) { "BLE packet too large" }
        return result
    }

    private fun hmac(data: ByteArray, key: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }
}
