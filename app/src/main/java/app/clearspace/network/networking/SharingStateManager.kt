package app.clearspace.network.networking

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharingStateManager(private val context: Context) {
    private val _isSharingEnabled = MutableStateFlow(false)
    val isSharingEnabled: StateFlow<Boolean> = _isSharingEnabled.asStateFlow()

    private var discoveryManager: app.clearspace.network.discovery.DiscoveryManager? = null

    private val _isRootWarningActive = MutableStateFlow(false)
    val isRootWarningActive: StateFlow<Boolean> = _isRootWarningActive.asStateFlow()

    fun setRootWarningActive(active: Boolean) {
        _isRootWarningActive.value = active
    }

    fun enableSharing() {
        if (_isSharingEnabled.value) return
        _isSharingEnabled.value = true
        
        try {
            // Start the foreground service
            val intent = Intent(context, app.clearspace.network.discovery.DiscoveryForegroundService::class.java)
            context.startForegroundService(intent)
        } catch (e: Exception) {
            _isSharingEnabled.value = false
        }
        
        val protocols = listOf(
            app.clearspace.network.discovery.BleDiscovery(context),
            app.clearspace.network.discovery.WifiDirectDiscovery(context),
            app.clearspace.network.discovery.NsdDiscovery(context)
        )
        discoveryManager = app.clearspace.network.discovery.DiscoveryManager(protocols)
        
        val packet = app.clearspace.network.discovery.PresencePacket(
            peerId = java.util.UUID.randomUUID().toString().substring(0, 8),
            hasProfile = true,
            categoryBitmask = 0L,
            tcpPort = 8080
        )
        discoveryManager?.start(packet)
    }

    fun disableSharing() {
        if (!_isSharingEnabled.value) return
        _isSharingEnabled.value = false
        
        // Stop the foreground service
        val intent = Intent(context, app.clearspace.network.discovery.DiscoveryForegroundService::class.java)
        context.stopService(intent)
        
        discoveryManager?.stop()
        discoveryManager = null
    }
}
