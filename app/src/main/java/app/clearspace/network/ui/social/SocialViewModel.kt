package app.clearspace.network.ui.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.clearspace.network.crypto.KeyManager
import app.clearspace.network.social.DiscoveredProfile
import app.clearspace.network.social.SocialDiscoveryAgent

class SocialViewModel(application: Application) : AndroidViewModel(application) {
    
    private val agent = SocialDiscoveryAgent(application, KeyManager(application))
    
    // For now we'll just expose the agent's flow.
    // In a real app we'd connect this to the DiscoveryManager running in the service.
    val nearbyProfiles: StateFlow<List<DiscoveredProfile>> = agent.nearbyProfiles

    fun addAcquaintance(peerId: String) {
        agent.addAcquaintance(peerId)
    }

    fun removeAcquaintance(peerId: String) {
        agent.removeAcquaintance(peerId)
    }
}
