package org.example.aetherworks.discovery

import kotlinx.coroutines.flow.StateFlow

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

    fun start(presencePacket: PresencePacket) {
        protocols.forEach { it.startDiscovery(presencePacket) }
    }

    fun stop() {
        protocols.forEach { it.stopDiscovery() }
    }
}
