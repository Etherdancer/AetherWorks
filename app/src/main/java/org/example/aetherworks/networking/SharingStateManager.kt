package org.example.aetherworks.networking

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharingStateManager(private val context: Context) {
    private val _isSharingEnabled = MutableStateFlow(false)
    val isSharingEnabled: StateFlow<Boolean> = _isSharingEnabled.asStateFlow()

    fun enableSharing() {
        if (_isSharingEnabled.value) return
        _isSharingEnabled.value = true
        
        // Start the foreground service
        val intent = Intent(context, org.example.aetherworks.discovery.DiscoveryForegroundService::class.java)
        context.startForegroundService(intent)
        
        // TODO: Start the DiscoveryManager and P2P services here when implemented
    }

    fun disableSharing() {
        if (!_isSharingEnabled.value) return
        _isSharingEnabled.value = false
        
        // Stop the foreground service
        val intent = Intent(context, org.example.aetherworks.discovery.DiscoveryForegroundService::class.java)
        context.stopService(intent)
        
        // TODO: Stop all discovery, close connections, unregister network services here
    }
}
