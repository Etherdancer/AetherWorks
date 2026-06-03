package org.example.aetherworks.discovery

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class WifiDirectDiscovery(private val context: Context) : DiscoveryProtocol {

    companion object {
        const val SERVICE_INSTANCE = "_p2psync"
        const val SERVICE_REG_TYPE = "_presence._tcp"
    }

    private val wifiP2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _discoveredPeers = MutableStateFlow<List<PresencePacket>>(emptyList())
    override val discoveredPeers: StateFlow<List<PresencePacket>> = _discoveredPeers.asStateFlow()

    private var serviceRequest: WifiP2pDnsSdServiceRequest? = null

    init {
        try {
            channel = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)
        } catch (e: SecurityException) {
            channel = null
        }
    }

    override fun startDiscovery(presencePacket: PresencePacket) {
        if (wifiP2pManager == null || channel == null) return

        try {
            // 1. Broadcast our service
            val record = mapOf(
                "peerId" to presencePacket.peerId,
                "hasProfile" to presencePacket.hasProfile.toString(),
                "port" to presencePacket.tcpPort.toString()
            )
            val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE,
                SERVICE_REG_TYPE,
                record
            )

            wifiP2pManager.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}
                override fun onFailure(reason: Int) {}
            })

            // 2. Discover others
            wifiP2pManager.setDnsSdResponseListeners(channel, { instanceName, registrationType, srcDevice ->
                // Service available
            }, { fullDomainName, recordMap, srcDevice ->
                // FIX MC3: Strict prefix check instead of loose contains(), and UUID validation
                if (fullDomainName.startsWith(SERVICE_INSTANCE, ignoreCase = true) && recordMap.containsKey("peerId")) {
                    val peerId = recordMap["peerId"]
                    
                    // Validate UUID format
                    val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                    if (peerId == null || !uuidRegex.matches(peerId)) return@setDnsSdResponseListeners

                    val hasProfile = recordMap["hasProfile"]?.toBoolean() ?: false
                    val port = recordMap["port"]?.toIntOrNull() ?: 0

                    if (true) { // peerId is checked above
                        val packet = PresencePacket(peerId, hasProfile, 0L, port)
                        val current = _discoveredPeers.value.toMutableList()
                        if (!current.any { it.peerId == packet.peerId }) {
                            current.add(packet)
                            _discoveredPeers.value = current
                            
                            val config = android.net.wifi.p2p.WifiP2pConfig()
                            config.deviceAddress = srcDevice.deviceAddress
                            wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    wifiP2pManager.requestConnectionInfo(channel) { info ->
                                        if (info.groupFormed && !info.isGroupOwner) {
                                            val groupOwnerAddress = info.groupOwnerAddress.hostAddress
                                            val updatedCurrent = _discoveredPeers.value.map { 
                                                if (it.peerId == peerId) it.copy(ip = groupOwnerAddress) else it 
                                            }
                                            _discoveredPeers.value = updatedCurrent
                                        }
                                    }
                                }
                                override fun onFailure(reason: Int) {}
                            })
                        }
                    }
                }
            })

            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
            wifiP2pManager.addServiceRequest(channel, serviceRequest, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    wifiP2pManager.discoverServices(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {}
                        override fun onFailure(reason: Int) {}
                    })
                }
                override fun onFailure(reason: Int) {}
            })
        } catch (e: SecurityException) {
            // Permission denied, fallback to other transports
        }
    }

    override fun stopDiscovery() {
        if (wifiP2pManager == null || channel == null) return

        try {
            wifiP2pManager.clearLocalServices(channel, null)
            wifiP2pManager.clearServiceRequests(channel, null)
        } catch (e: SecurityException) {
            // Permission denied
        }
    }
}
