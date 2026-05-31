package org.example.aetherworks.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import org.example.aetherworks.discovery.PeerRepository

class NsdDiscovery(context: Context) : DiscoveryProtocol {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _discoveredPeers = MutableStateFlow<List<PresencePacket>>(emptyList())
    override val discoveredPeers: StateFlow<List<PresencePacket>> = _discoveredPeers.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private val serviceNameSuffix = UUID.randomUUID().toString().substring(0, 8)

    companion object {
        const val SERVICE_TYPE = "_aetherworks._tcp."
        const val BASE_SERVICE_NAME = "AetherWorksPeer"
    }

    override fun startDiscovery(presencePacket: PresencePacket) {
        val uniqueServiceName = "$BASE_SERVICE_NAME-$serviceNameSuffix"

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = uniqueServiceName
            serviceType = SERVICE_TYPE
            port = presencePacket.tcpPort
            
            // Encode presence packet into TXT record.
            // mDNS TXT record size limits usually apply to individual key-value pairs (255 bytes max).
            val jsonString = Json.encodeToString(presencePacket)
            setAttribute("presence", jsonString)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                // Ignore our own broadcast
                if (service.serviceName != uniqueServiceName) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val presenceBytes = serviceInfo.attributes["presence"]
                            if (presenceBytes != null) {
                                try {
                                    val jsonString = String(presenceBytes)
                                    val packet = Json.decodeFromString<PresencePacket>(jsonString)
                                    
                                    // Add to our list, replacing existing peer if necessary
                                    val current = _discoveredPeers.value.toMutableList()
                                    current.removeAll { it.peerId == packet.peerId }
                                    current.add(packet)
                                    _discoveredPeers.value = current
                                    PeerRepository.updatePeers(current)
                                } catch (e: Exception) {
                                    // Ignore invalid packets or older protocol versions
                                }
                            }
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                // Optional: remove peer if its service is lost. In practice, TXT records don't give us
                // the peerId without resolving again, so we typically rely on application-level heartbeats.
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun stopDiscovery() {
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            // Service already stopped or not registered
        }
        
        registrationListener = null
        discoveryListener = null
        _discoveredPeers.value = emptyList()
    }
}
