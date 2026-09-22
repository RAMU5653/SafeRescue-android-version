package com.saferescue.app.core.hardware.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Minimal BLE GATT writer for a previously paired/configured ESP32.
 * Discovery/scanning is deliberately outside the emergency path.
 */
class AndroidBluetoothTransport(
    private val gatt: BluetoothGatt,
    private val writeCharacteristic: BluetoothGattCharacteristic,
    private val signingKeyProvider: () -> ByteArray,
) : BluetoothTransport {
    override suspend fun send(packet: BluetoothEmergencyPacket): BluetoothResult = withContext(Dispatchers.IO) {
        val key = signingKeyProvider()
        val payload = BluetoothEmergencyCodec.encode(packet, key)
        return@withContext try {
            writeCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            writeCharacteristic.value = payload
            if (gatt.writeCharacteristic(writeCharacteristic)) BluetoothResult.Sent
            else BluetoothResult.Failed("GATT write was rejected")
        } catch (security: SecurityException) {
            BluetoothResult.Failed("Bluetooth permission denied")
        } catch (t: Throwable) {
            BluetoothResult.Failed("BLE transport failure")
        }
    }

    override fun status(): BluetoothStatus = BluetoothStatus.CONNECTED

    companion object {
        /** Reserved UUIDs for the SafeRescue ESP32 BLE profile. */
        val SERVICE_UUID: UUID = UUID.fromString("7f7a1000-5afe-4c72-9b7b-534146455352")
        val EMERGENCY_WRITE_UUID: UUID = UUID.fromString("7f7a1001-5afe-4c72-9b7b-534146455352")
    }
}
