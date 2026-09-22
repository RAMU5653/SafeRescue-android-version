package com.saferescue.app.core.hardware.bluetooth

/** Hardware boundary. A SafeRescue emergency must remain fully functional when BLE is absent. */
interface BluetoothTransport {
    suspend fun send(packet: BluetoothEmergencyPacket): BluetoothResult
    fun status(): BluetoothStatus
}

class NotConfiguredBluetoothTransport : BluetoothTransport {
    override suspend fun send(packet: BluetoothEmergencyPacket): BluetoothResult = BluetoothResult.NotConfigured
    override fun status(): BluetoothStatus = BluetoothStatus.NOT_CONFIGURED
}
