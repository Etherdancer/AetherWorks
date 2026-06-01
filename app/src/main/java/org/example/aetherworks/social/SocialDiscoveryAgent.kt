package org.example.aetherworks.social

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.aetherworks.discovery.PresencePacket
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.discovery.P2PClient

data class DiscoveredProfile(
    val peerId: String,
    val alias: String, // from fetching profile, but we don't have this yet, just use peerId for now
    val hasProfile: Boolean,
    val isAcquaintance: Boolean
)

class SocialDiscoveryAgent(private val context: Context, private val keyManager: KeyManager) {
    
    private val _nearbyProfiles = MutableStateFlow<List<DiscoveredProfile>>(emptyList())
    val nearbyProfiles: StateFlow<List<DiscoveredProfile>> = _nearbyProfiles.asStateFlow()

    fun updatePeers(peers: List<PresencePacket>) {
        val current = _nearbyProfiles.value.toMutableList()
        peers.forEach { packet ->
            val existing = current.find { it.peerId == packet.peerId }
            if (existing == null) {
                current.add(DiscoveredProfile(
                    peerId = packet.peerId,
                    alias = "Peer ${packet.peerId}",
                    hasProfile = packet.hasProfile,
                    isAcquaintance = false
                ))
                
                if (packet.hasProfile && packet.ip != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val fetchedProfile = P2PClient.fetchProfile(packet.ip!!, packet.tcpPort)
                        if (fetchedProfile != null) {
                            val updatedList = _nearbyProfiles.value.map { 
                                if (it.peerId == packet.peerId) it.copy(alias = fetchedProfile.alias) else it 
                            }
                            _nearbyProfiles.value = updatedList
                        }
                    }
                }
            }
        }
        _nearbyProfiles.value = current
    }

    fun addAcquaintance(peerId: String) {
        val current = _nearbyProfiles.value.map { 
            if (it.peerId == peerId) it.copy(isAcquaintance = true) else it 
        }
        _nearbyProfiles.value = current
        
        // TODO: Save to database or SharedPreferences so it persists
    }
}
