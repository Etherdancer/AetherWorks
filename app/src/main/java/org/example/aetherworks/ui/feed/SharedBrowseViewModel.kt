package org.example.aetherworks.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import org.example.aetherworks.crypto.GroupEncryption
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.discovery.ContentHeader
import org.example.aetherworks.discovery.PeerRepository
import org.example.aetherworks.discovery.P2PClient
import org.example.aetherworks.reputation.ReputationAgent
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.TrustLevel
import org.example.aetherworks.storage.db.entity.Visibility

class SharedBrowseViewModel(application: Application) : AndroidViewModel(application) {
    private val _sharedHeaders = MutableStateFlow<List<Pair<String, ContentHeader>>>(emptyList()) // Pair of IP/Port to Header
    val sharedHeaders: StateFlow<List<Pair<String, ContentHeader>>> = _sharedHeaders.asStateFlow()
    
    private val _loadingHeader = MutableStateFlow(false)
    val loadingHeader: StateFlow<Boolean> = _loadingHeader.asStateFlow()

    private val _viewingContent = MutableStateFlow<ContentUnit?>(null)
    val viewingContent: StateFlow<ContentUnit?> = _viewingContent.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    enum class SourceFilter { ALL, ACQUAINTANCES, TRUSTED }
    private val _sourceFilter = MutableStateFlow(SourceFilter.ALL)
    val sourceFilter: StateFlow<SourceFilter> = _sourceFilter.asStateFlow()

    private val db = AetherDatabase.getPrivateDatabase()
    private val sharedDb = AetherDatabase.getSharedDatabase()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSourceFilter(filter: SourceFilter) {
        _sourceFilter.value = filter
    }

    init {
        viewModelScope.launch {
            val peersFlow = db.peerDao().getAllPeers()
            combine(
                PeerRepository.discoveredPeers,
                _sourceFilter,
                peersFlow
            ) { discoveredPeers, filter, knownPeers ->
                val newHeaders = mutableListOf<Pair<String, ContentHeader>>()
                for (peer in discoveredPeers) {
                    val ip = peer.ip
                    if (ip != null && peer.tcpPort > 0) {
                        try {
                            val indexes = P2PClient.fetchIndex(ip, peer.tcpPort)
                            if (indexes != null) {
                                for (header in indexes) {
                                    val isAcquaintance = knownPeers.any { it.alias == header.authorAlias }
                                    val isTrusted = knownPeers.any { it.alias == header.authorAlias && it.trustLevel != TrustLevel.ACQUAINTANCE }
                                    
                                    val include = when (filter) {
                                        SourceFilter.ALL -> true
                                        SourceFilter.ACQUAINTANCES -> isAcquaintance
                                        SourceFilter.TRUSTED -> isTrusted
                                    }
                                    if (include) {
                                        newHeaders.add(Pair("$ip:${peer.tcpPort}", header))
                                    }
                                }
                            }
                        } catch(e: Exception) {
                            // Ignored or log
                        }
                    }
                }
                newHeaders
            }.collect { filtered ->
                _sharedHeaders.value = filtered
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
                    var content = P2PClient.fetchContent(ip, port, hash)
                    if (content != null && content.visibility == Visibility.GROUP && content.recipientKeyMapJson != null) {
                        val keyManager = KeyManager(getApplication())
                        val myX25519Priv = keyManager.getOrGenerateEncryptionIdentity().first
                        val unwrappedAesKey = GroupEncryption.unwrapKey(content.recipientKeyMapJson, myX25519Priv)
                        
                        if (unwrappedAesKey != null) {
                            try {
                                val encryptedPayload = android.util.Base64.decode(content.body, android.util.Base64.DEFAULT)
                                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                                val iv = encryptedPayload.copyOfRange(0, 12)
                                val ciphertext = encryptedPayload.copyOfRange(12, encryptedPayload.size)
                                val keySpec = javax.crypto.spec.SecretKeySpec(unwrappedAesKey, "AES")
                                val gcmSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
                                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, gcmSpec)
                                val decryptedBody = cipher.doFinal(ciphertext)
                                content = content.copy(body = String(decryptedBody))
                            } catch (e: Exception) {
                                content = content.copy(body = "[Decryption Failed: Invalid Payload]")
                            }
                        } else {
                            content = content.copy(body = "[Decryption Failed: You are not in this group or key unwrapping failed]")
                        }
                    }
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

    fun vote(unit: ContentUnit, isLike: Boolean) {
        viewModelScope.launch {
            val repAgent = ReputationAgent(getApplication(), KeyManager(getApplication()))
            if (!repAgent.canVote()) return@launch

            val token = repAgent.generateVoteToken(unit.contentHash)
            
            // For remote content, save a stub in Shared DB so the vote can be gossiped
            val updatedUnit = if (isLike) {
                unit.copy(likeTokens = unit.likeTokens + token)
            } else {
                unit.copy(dislikeTokens = unit.dislikeTokens + token)
            }
            
            sharedDb.contentDao().insert(updatedUnit.copy(importCount = 0)) // Save as stub if not exists
            
            // Update viewing content
            if (_viewingContent.value?.contentHash == unit.contentHash) {
                _viewingContent.value = updatedUnit
            }
        }
    }
}
