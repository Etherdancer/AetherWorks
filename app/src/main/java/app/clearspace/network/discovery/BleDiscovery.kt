package app.clearspace.network.discovery

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.UUID

class BleDiscovery(private val context: Context) : DiscoveryProtocol {

    companion object {
        // A unique Service UUID for AetherWorks BLE Discovery
        val SERVICE_UUID: ParcelUuid = ParcelUuid(UUID.fromString("0000A1B2-0000-1000-8000-00805F9B34FB"))
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    private val _discoveredPeers = MutableStateFlow<List<PresencePacket>>(emptyList())
    override val discoveredPeers: StateFlow<List<PresencePacket>> = _discoveredPeers.asStateFlow()

    private var isAdvertising = false
    private var isScanning = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
        }
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.scanRecord?.getServiceData(SERVICE_UUID)?.let { data ->
                val packet = parsePacket(data)
                if (packet != null) {
                    val current = _discoveredPeers.value.toMutableList()
                    if (!current.any { it.peerId == packet.peerId }) {
                        current.add(packet)
                        _discoveredPeers.value = current
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startDiscovery(presencePacket: PresencePacket) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        try {
            startAdvertising(presencePacket)
            startScanning()
        } catch (e: SecurityException) {
            // Permission denied, silently fail discovery instead of crashing
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        try {
            if (isAdvertising) {
                bleAdvertiser?.stopAdvertising(advertiseCallback)
                isAdvertising = false
            }
            if (isScanning) {
                bleScanner?.stopScan(scanCallback)
                isScanning = false
            }
        } catch (e: SecurityException) {
            // Permission denied
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising(packet: PresencePacket) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(SERVICE_UUID)
            .addServiceData(SERVICE_UUID, buildPacketData(packet))
            .build()

        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        isAdvertising = true
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        val filter = ScanFilter.Builder()
            .setServiceUuid(SERVICE_UUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        bleScanner?.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
    }

    private fun buildPacketData(packet: PresencePacket): ByteArray {
        // Format: [1 byte hasProfile] [4 bytes tcpPort] [8 bytes peerId string]
        val buffer = ByteBuffer.allocate(13)
        buffer.put((if (packet.hasProfile) 1 else 0).toByte())
        buffer.putInt(packet.tcpPort)
        val idBytes = packet.peerId.toByteArray().take(8).toByteArray()
        buffer.put(idBytes)
        // Pad if needed
        while (buffer.position() < 13) {
            buffer.put(0.toByte())
        }
        return buffer.array()
    }

    private fun parsePacket(data: ByteArray): PresencePacket? {
        if (data.size < 13) return null
        val buffer = ByteBuffer.wrap(data)
        val hasProfile = buffer.get().toInt() == 1
        val tcpPort = buffer.getInt()
        val idBytes = ByteArray(8)
        buffer.get(idBytes)
        val peerId = String(idBytes).trimEnd('\u0000')

        return PresencePacket(
            peerId = peerId,
            hasProfile = hasProfile,
            categoryBitmask = 0L,
            tcpPort = tcpPort
        )
    }

    @SuppressLint("MissingPermission")
    fun setScanMode(mode: Int) {
        if (!isScanning || bleScanner == null) return
        try {
            bleScanner?.stopScan(scanCallback)
            val filter = ScanFilter.Builder()
                .setServiceUuid(SERVICE_UUID)
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(mode)
                .build()
            bleScanner?.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            // Permission denied
        }
    }
}
