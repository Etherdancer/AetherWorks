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
                            if (!current.any { it.peerId == peer.peerId }) {
                                current.add(peer)
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
