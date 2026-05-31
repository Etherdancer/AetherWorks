package org.example.aetherworks.ui.social

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.example.aetherworks.discovery.PeerRepository
import org.example.aetherworks.discovery.PresencePacket

class SocialViewModel : ViewModel() {
    val discoveredPeers: StateFlow<List<PresencePacket>> = PeerRepository.discoveredPeers
}
