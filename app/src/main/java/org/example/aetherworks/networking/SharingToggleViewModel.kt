package org.example.aetherworks.networking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SharingToggleViewModel(private val sharingStateManager: SharingStateManager) : ViewModel() {
    val isSharingEnabled = sharingStateManager.isSharingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    fun enableSharing() = sharingStateManager.enableSharing()
    fun disableSharing() = sharingStateManager.disableSharing()
}
