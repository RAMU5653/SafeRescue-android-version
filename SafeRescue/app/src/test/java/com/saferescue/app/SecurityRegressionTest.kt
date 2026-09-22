package com.saferescue.app

import com.saferescue.app.core.backend.SecureBackendClient
import com.saferescue.app.core.backend.UploadOutcome
import com.saferescue.app.core.hardware.bluetooth.BluetoothEmergencyCodec
import com.saferescue.app.core.hardware.bluetooth.BluetoothEmergencyPacket
import com.saferescue.app.core.hardware.bluetooth.BluetoothEmergencyState
import com.saferescue.app.core.hardware.bluetooth.BluetoothMessageType
import com.saferescue.app.core.hardware.lora.LoRaEmergencyPacket
import com.saferescue.app.core.hardware.lora.LoRaEmergencyState
import com.saferescue.app.core.hardware.lora.LoRaMessageType
import com.saferescue.app.core.hardware.lora.LoRaPacketCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityRegressionTest {
    private val key = "test-only-secret-for-security-tests".toByteArray()

    @Test
    fun loraPacket_roundTripsAndRejectsTampering() {
        val packet = LoRaEmergencyPacket(
            LoRaMessageType.SOS, "phone-01", 1_700_000_000_000L,
            LoRaEmergencyState.ACTIVE, 87, 17.45, 78.38, 8.5f, "nonce-1234"
        )
        val encoded = LoRaPacketCodec.encode(packet, key)
        assertEquals(packet, LoRaPacketCodec.decode(encoded, key))
        val tampered = encoded.copyOf().also { it[encoded.lastIndex - 2] = if (it[encoded.lastIndex - 2].toInt() == '0'.code) '1'.code.toByte() else '0'.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { LoRaPacketCodec.decode(tampered, key) }
    }

    @Test
    fun bluetoothPacket_isBoundedAndAuthenticated() {
        val packet = BluetoothEmergencyPacket(
            BluetoothMessageType.SOS, "phone-01", 1_700_000_000_000L,
            BluetoothEmergencyState.ACTIVE, 50, 17.45, 78.38, "nonce-1234"
        )
        val encoded = BluetoothEmergencyCodec.encode(packet, key)
        assertTrue(encoded.size <= 512)
        assertTrue(encoded.decodeToString().contains("mac="))
    }

    @Test
    fun backendRejectsUnsafeTransportConfigurationAndBadIdempotencyKey() {
        val http = SecureBackendClient("http://example.invalid", "token")
            .uploadIncident("{}", "valid-key-123456")
        assertEquals(UploadOutcome.FATAL, http.outcome)

        val badKey = SecureBackendClient("https://example.invalid", "token")
            .uploadIncident("{}", "bad key")
        assertEquals(UploadOutcome.FATAL, badKey.outcome)
    }

    @Test
    fun backendRejectsCredentialsEmbeddedInBaseUrl() {
        val result = SecureBackendClient("https://user:pass@example.invalid", "token")
            .uploadIncident("{}", "valid-key-123456")
        assertEquals(UploadOutcome.FATAL, result.outcome)
    }
}
