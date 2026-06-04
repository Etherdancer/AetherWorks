package app.clearspace.network.social

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.clearspace.network.discovery.PresencePacket
import app.clearspace.network.crypto.KeyManager
import app.clearspace.network.discovery.P2PClient

data class DiscoveredProfile(
    val peerId: String,
    val alias: String, // from fetching profile, but we don't have this yet, just use peerId for now
    val hasProfile: Boolean,
    val isAcquaintance: Boolean
)

class SocialDiscoveryAgent(private val context: Context, private val keyManager: KeyManager) {
    
    private val _nearbyProfiles = MutableStateFlow<List<DiscoveredProfile>>(emptyList())
    val nearbyProfiles: StateFlow<List<DiscoveredProfile>> = _nearbyProfiles.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = app.clearspace.network.storage.db.AetherDatabase.getSharedDatabase()
                db.peerDao().getAllPeers().collect { peers ->
                    val currentList = _nearbyProfiles.value.toMutableList()
                    peers.forEach { peer ->
                        val existing = currentList.find { it.peerId == peer.publicKeyBase64 }
                        if (existing != null) {
                            val index = currentList.indexOf(existing)
                            currentList[index] = existing.copy(isAcquaintance = peer.trustLevel == app.clearspace.network.storage.db.entity.TrustLevel.ACQUAINTANCE)
                        } else {
                            currentList.add(DiscoveredProfile(
                                peerId = peer.publicKeyBase64,
                                alias = peer.alias,
                                hasProfile = true,
                                isAcquaintance = peer.trustLevel == app.clearspace.network.storage.db.entity.TrustLevel.ACQUAINTANCE
                            ))
                        }
                    }
                    _nearbyProfiles.value = currentList
                }
            } catch (e: Exception) {
                // DB might not be ready
            }
        }
    }

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
            } else {
                if (existing.alias == "Peer ${packet.peerId}" && packet.hasProfile && packet.ip != null) {
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
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = app.clearspace.network.storage.db.AetherDatabase.getSharedDatabase()
                val profile = current.find { it.peerId == peerId } ?: return@launch
                val peer = app.clearspace.network.storage.db.entity.KnownPeer(
                    publicKeyBase64 = peerId,
                    alias = profile.alias,
                    trustLevel = app.clearspace.network.storage.db.entity.TrustLevel.ACQUAINTANCE
                )
                db.peerDao().insert(peer)
            } catch (e: Exception) {
                // DB might not be ready
            }
        }
    }

    fun removeAcquaintance(peerId: String) {
        val current = _nearbyProfiles.value.map { 
            if (it.peerId == peerId) it.copy(isAcquaintance = false) else it 
        }
        _nearbyProfiles.value = current
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = app.clearspace.network.storage.db.AetherDatabase.getSharedDatabase()
                db.peerDao().delete(peerId)
            } catch (e: Exception) {
                // DB might not be ready
            }
        }
    }
}
