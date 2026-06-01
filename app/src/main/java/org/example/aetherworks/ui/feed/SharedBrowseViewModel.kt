package org.example.aetherworks.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.aetherworks.discovery.ContentHeader
import org.example.aetherworks.discovery.PeerRepository
import org.example.aetherworks.discovery.P2PClient
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.Visibility

class SharedBrowseViewModel : ViewModel() {
    private val _sharedHeaders = MutableStateFlow<List<Pair<String, ContentHeader>>>(emptyList()) // Pair of IP/Port to Header
    val sharedHeaders: StateFlow<List<Pair<String, ContentHeader>>> = _sharedHeaders.asStateFlow()
    
    private val _loadingHeader = MutableStateFlow(false)
    val loadingHeader: StateFlow<Boolean> = _loadingHeader.asStateFlow()

    private val _viewingContent = MutableStateFlow<ContentUnit?>(null)
    val viewingContent: StateFlow<ContentUnit?> = _viewingContent.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        viewModelScope.launch {
            PeerRepository.discoveredPeers.collect { peers ->
                val newHeaders = mutableListOf<Pair<String, ContentHeader>>()
                for (peer in peers) {
                    val ip = peer.ip
                    if (ip != null && peer.tcpPort > 0) {
                        try {
                            val indexes = P2PClient.fetchIndex(ip, peer.tcpPort)
                            if (indexes != null) {
                                indexes.forEach { header ->
                                    newHeaders.add(Pair("$ip:${peer.tcpPort}", header))
                                }
                            }
                        } catch(e: Exception) {
                            // Ignored or log
                        }
                    }
                }
                _sharedHeaders.value = newHeaders
            }
        }
    }
    
    fun clearViewingContent() {
        _viewingContent.value = null
    }

    fun openContent(ipPort: String, hash: String) {
        viewModelScope.launch {
            _loadingHeader.value = true
            val parts = ipPort.split(":")
            if (parts.size == 2) {
                val ip = parts[0]
                val port = parts[1].toIntOrNull() ?: 0
                try {
                    val content = P2PClient.fetchContent(ip, port, hash)
                    _viewingContent.value = content
                } catch(e: Exception) {
                    // Log
                }
            }
            _loadingHeader.value = false
        }
    }
    
    fun saveToPrivateLibrary(unit: ContentUnit) {
        viewModelScope.launch {
            val privateUnit = unit.copy(visibility = Visibility.PRIVATE)
            AetherDatabase.getPrivateDatabase().contentDao().insert(privateUnit)
        }
    }
}
