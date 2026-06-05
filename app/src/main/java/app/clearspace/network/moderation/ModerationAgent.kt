package app.clearspace.network.moderation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import app.clearspace.network.storage.db.AetherDatabase
import app.clearspace.network.storage.db.entity.BlacklistEntry
import com.google.firebase.firestore.FirebaseFirestore
import androidx.room.withTransaction

class ModerationAgent(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun syncBlacklist() {
        withContext(Dispatchers.IO) {
            try {
                // Fetch the blacklisted hashes from Firestore
                val result = db.collection("blacklisted_hashes").get().await()
                val bannedHashes = result.documents.mapNotNull { it.id }
                
                if (bannedHashes.isNotEmpty()) {
                    // Persist to local cache
                    val privateDb = AetherDatabase.getPrivateDatabase()
                    privateDb.withTransaction {
                        privateDb.blacklistDao().clear()
                        privateDb.blacklistDao().insertAll(bannedHashes.map { BlacklistEntry(it, System.currentTimeMillis()) })
                    }
                    applyBlacklist(bannedHashes)
                }
            } catch (e: Exception) {
                // Fail CLOSED: apply cached blacklist even without network
                try {
                    val privateDb = AetherDatabase.getPrivateDatabase()
                    val cached = privateDb.blacklistDao().getAll().map { it.hash }
                    if (cached.isNotEmpty()) applyBlacklist(cached)
                } catch (dbEx: Exception) {
                    dbEx.printStackTrace()
                }
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
