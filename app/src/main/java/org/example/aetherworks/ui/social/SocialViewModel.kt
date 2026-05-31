package org.example.aetherworks.ui.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.social.DiscoveredProfile
import org.example.aetherworks.social.SocialDiscoveryAgent

class SocialViewModel(application: Application) : AndroidViewModel(application) {
    
    private val agent = SocialDiscoveryAgent(application, KeyManager(application))
    
    // For now we'll just expose the agent's flow.
    // In a real app we'd connect this to the DiscoveryManager running in the service.
    val nearbyProfiles: StateFlow<List<DiscoveredProfile>> = agent.nearbyProfiles

    fun addAcquaintance(peerId: String) {
        agent.addAcquaintance(peerId)
    }
}
