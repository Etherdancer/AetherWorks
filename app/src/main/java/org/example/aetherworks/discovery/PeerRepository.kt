package org.example.aetherworks.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton repository holding the live state of nearby discovered peers.
 * Updated by DiscoveryManager/ForegroundService, observed by the UI (SocialScreen).
 */
object PeerRepository {
    private val _discoveredPeers = MutableStateFlow<List<PresencePacket>>(emptyList())
    val discoveredPeers: StateFlow<List<PresencePacket>> = _discoveredPeers.asStateFlow()

    fun updatePeers(peers: List<PresencePacket>) {
        _discoveredPeers.value = peers
    }
}
