package com.saferescue.app

import com.saferescue.app.core.hardware.lora.LoRaEmergencyPacket
import com.saferescue.app.core.hardware.lora.LoRaEmergencyState
import com.saferescue.app.core.hardware.lora.LoRaMessageType
import com.saferescue.app.core.hardware.lora.LoRaPacketCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LoRaPacketCodecTest {
    private val secret = "test-secret".toByteArray()
    private val packet = LoRaEmergencyPacket(
        LoRaMessageType.SOS, "device-01", 1700000000000L,
        LoRaEmergencyState.ACTIVE, 77, 17.5, 78.4, 12.5f, "nonce1234"
    )

    @Test fun roundTrip() {
        assertEquals(packet, LoRaPacketCodec.decode(LoRaPacketCodec.encode(packet, secret), secret))
    }

    @Test fun wrongSecretRejected() {
        val encoded = LoRaPacketCodec.encode(packet, secret)
        assertThrows(IllegalArgumentException::class.java) {
            LoRaPacketCodec.decode(encoded, "wrong".toByteArray())
        }
    }

    @Test fun emptySecretRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LoRaPacketCodec.encode(packet, byteArrayOf())
        }
    }
}
