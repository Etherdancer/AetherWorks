package app.clearspace.network.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.clearspace.network.storage.db.AetherDatabase
import app.clearspace.network.storage.db.entity.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.clearspace.network.crypto.KeyManager
import app.clearspace.network.persona.PersonaAgent

data class ChatMessage(
    val id: Long,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long,
    val isDelivered: Boolean
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var myPublicKey: String = ""
    private var peerPublicKey: String = ""
    private lateinit var keyManager: KeyManager

    fun initChat(context: android.content.Context, peerKey: String) {
        peerPublicKey = peerKey
        keyManager = KeyManager(context)
        val personaAgent = PersonaAgent(context, keyManager)
        myPublicKey = personaAgent.publicKeyBase64

        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val db = AetherDatabase.getPrivateDatabase()
            val rawMessages = db.messageDao().getConversation(peerPublicKey)
            
            // Decrypt payload
            val chatMessages = rawMessages.mapNotNull { msg ->
                try {
                    val isFromMe = msg.senderPublicKey == myPublicKey
                    
                    // TODO: Replace with actual E2EE ECDH decryption. 
                    // For now, since this is a prototype phase and we might not have the full Diffie-Hellman setup in KeyManager yet, 
                    // we'll assume the payload is just Base64 encoded or plaintext if not encrypted properly.
                    val decodedText = try {
                        String(android.util.Base64.decode(msg.encryptedPayload, android.util.Base64.URL_SAFE))
                    } catch (e: Exception) {
                        msg.encryptedPayload
                    }

                    ChatMessage(
                        id = msg.id,
                        text = decodedText,
                        isFromMe = isFromMe,
                        timestamp = msg.timestamp,
                        isDelivered = msg.delivered
                    )
                } catch (e: Exception) {
                    null // Skip messages that can't be decrypted
                }
            }
            _messages.value = chatMessages
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val db = AetherDatabase.getPrivateDatabase()
            
            // TODO: Proper encryption with peer's public key
            val encryptedPayload = android.util.Base64.encodeToString(text.toByteArray(), android.util.Base64.URL_SAFE)
            
            val msg = Message(
                senderPublicKey = myPublicKey,
                receiverPublicKey = peerPublicKey,
                encryptedPayload = encryptedPayload,
                timestamp = System.currentTimeMillis(),
                delivered = false, // Will turn true when peer ACKs it
                ttl = 0
            )
            db.messageDao().insert(msg)
            
            loadMessages() // Reload to show the new message
            
            // Here we would enqueue the message to the P2P networking layer
        }
    }
}
