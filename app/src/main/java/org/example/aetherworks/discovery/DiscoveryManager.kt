package org.example.aetherworks.discovery

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base interface for all Android framework proximity technologies.
 * Implementations should not use Google Play Services.
 */
interface DiscoveryProtocol {
    fun startDiscovery(presencePacket: PresencePacket)
    fun stopDiscovery()
    
    /** Flow of currently active peers discovered by this protocol */
    val discoveredPeers: StateFlow<List<PresencePacket>>
}


/**
 * Orchestrator that cycles through and manages multiple discovery protocols.
 * As per architecture, cycles through BLE, Wi-Fi Direct, mDNS, etc.
 */
class DiscoveryManager(private val protocols: List<DiscoveryProtocol>) {

    private val _discoveredPeers = MutableStateFlow<List<PresencePacket>>(emptyList())
    val discoveredPeers: StateFlow<List<PresencePacket>> = _discoveredPeers.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            // Very naive aggregation for now
            // In a real app we'd combine all state flows from protocols
            protocols.forEach { protocol ->
                launch {
                    protocol.discoveredPeers.collect { peers ->
                        val current = _discoveredPeers.value.toMutableList()
                        peers.forEach { peer ->
                            val index = current.indexOfFirst { it.peerId == peer.peerId }
                            if (index == -1) {
                                current.add(peer)
                            } else {
                                val existing = current[index]
                                current[index] = existing.copy(
                                    ip = peer.ip ?: existing.ip,
                                    hasProfile = peer.hasProfile || existing.hasProfile,
                                    categoryBitmask = peer.categoryBitmask or existing.categoryBitmask,
                                    tcpPort = if (peer.tcpPort > 0) peer.tcpPort else existing.tcpPort,
                                    timestamp = maxOf(peer.timestamp, existing.timestamp)
                                )
                            }
                        }
                        _discoveredPeers.value = current
                    }
                }
            }
        }
    }

    fun start(presencePacket: PresencePacket) {
        protocols.forEach { it.startDiscovery(presencePacket) }
    }

    fun stop() {
        protocols.forEach { it.stopDiscovery() }
    }
}
