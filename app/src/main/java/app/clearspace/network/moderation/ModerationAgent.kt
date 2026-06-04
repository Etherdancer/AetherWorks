package app.clearspace.network.moderation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import app.clearspace.network.storage.db.AetherDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ModerationAgent(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun syncBlacklist() {
        withContext(Dispatchers.IO) {
            try {
                // Fetch the blacklisted hashes from Firestore
                val result = db.collection("blacklisted_hashes").get().await()
                val bannedHashes = result.documents.mapNotNull { it.id }
                
                if (bannedHashes.isNotEmpty()) {
                    applyBlacklist(bannedHashes)
                }
            } catch (e: Exception) {
                // Ignore network errors, fail open for a decentralized app
                e.printStackTrace()
            }
        }
    }

    private suspend fun applyBlacklist(bannedHashes: List<String>) {
        val sharedDb = AetherDatabase.getSharedDatabase()
        bannedHashes.forEach { hash ->
            // Delete locally if it exists
            sharedDb.contentDao().delete(hash)
        }
    }
}
