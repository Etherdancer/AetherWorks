package app.clearspace.network.discovery

import android.content.Context
import android.util.Log
import app.clearspace.network.crypto.KeyManager
import app.clearspace.network.persona.PersonaAgent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GlobalProfile(
    val id: String = "",
    val alias: String = "",
    val fcmToken: String = "",
    val avatarId: Int = 0,
    val bioSnippet: String = "",
    val timestamp: Long = 0L
)

class GlobalDiscoveryAgent(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val personaAgent = PersonaAgent(context, KeyManager(context))
    private val prefs = context.getSharedPreferences("aether_settings", Context.MODE_PRIVATE)

    companion object {
        const val TAG = "GlobalDiscovery"
        const val COLLECTION_DIRECTORY = "global_directory"
        const val COLLECTION_PINGS = "pings"
    }

    suspend fun updateDiscoverability() {
        val isDiscoverable = prefs.getBoolean("global_discoverable", false)
        val profile = personaAgent.getProfile() ?: return
        val pubKey = personaAgent.publicKeyBase64

        if (isDiscoverable) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val bioSnippet = profile.about?.value?.take(50) ?: ""
                
                val globalProfile = GlobalProfile(
                    id = pubKey,
                    alias = profile.alias,
                    fcmToken = token,
                    avatarId = profile.avatarId,
                    bioSnippet = bioSnippet,
                    timestamp = System.currentTimeMillis()
                )
                
                firestore.collection(COLLECTION_DIRECTORY)
                    .document(pubKey)
                    .set(globalProfile)
                    .await()
                Log.d(TAG, "Successfully registered in global directory.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register in global directory", e)
            }
        } else {
            try {
                firestore.collection(COLLECTION_DIRECTORY).document(pubKey).delete().await()
                Log.d(TAG, "Successfully removed from global directory.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove from global directory", e)
            }
        }
    }

    suspend fun searchByAlias(aliasQuery: String): List<GlobalProfile> {
        return try {
            val snapshot = firestore.collection(COLLECTION_DIRECTORY)
                .whereEqualTo("alias", aliasQuery)
                .limit(20)
                .get()
                .await()
            
            snapshot.toObjects(GlobalProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            emptyList()
        }
    }

    suspend fun pingUser(targetPubKey: String) {
        val myProfile = personaAgent.getProfile() ?: return
        try {
            val pingData = mapOf(
                "senderId" to personaAgent.publicKeyBase64,
                "senderAlias" to myProfile.alias,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_PINGS)
                .document(targetPubKey)
                .collection("incoming")
                .add(pingData)
                .await()
            Log.d(TAG, "Ping sent to $targetPubKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ping", e)
        }
    }
}
