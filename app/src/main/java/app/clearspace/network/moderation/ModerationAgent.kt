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

    private val prefs = context.getSharedPreferences("moderation_prefs", Context.MODE_PRIVATE)

    suspend fun syncBlacklist() {
        withContext(Dispatchers.IO) {
            try {
                // Client-Side Consensus Moderation
                // Fetch only NEW report tokens from Firebase globally
                val lastSync = prefs.getLong("last_sync_time", 0)
                val result = db.collectionGroup("reports")
                    .whereGreaterThan("timestamp", lastSync)
                    .get().await()
                
                val newTokensByHash = mutableMapOf<String, MutableSet<String>>()
                for (doc in result.documents) {
                    val hash = doc.reference.parent.parent?.id ?: continue
                    val token = doc.getString("reportToken") ?: continue
                    newTokensByHash.getOrPut(hash) { mutableSetOf() }.add(token)
                }

                if (newTokensByHash.isNotEmpty()) {
                    val sharedDb = AetherDatabase.getSharedDatabase()
                    val privateDb = AetherDatabase.getPrivateDatabase()
                    val newlyBlacklisted = mutableListOf<String>()

                    sharedDb.withTransaction {
                        for ((hash, newTokens) in newTokensByHash) {
                            val localUnit = sharedDb.contentDao().getByHash(hash)
                            if (localUnit != null) {
                                // Merge new report tokens
                                val updatedReports = (localUnit.reportTokens + newTokens).toMutableSet()
                                
                                // Cap at 10,000
                                if (updatedReports.size > 10000) {
                                    val toDrop = updatedReports.size - 10000
                                    updatedReports.removeAll(updatedReports.take(toDrop).toSet())
                                }

                                val updatedUnit = localUnit.copy(reportTokens = updatedReports)
                                
                                // Evaluate Dynamic Threshold: 5% of interactions, floor of 10
                                val totalInteractions = updatedUnit.likeTokens.size + updatedUnit.dislikeTokens.size
                                val dynamicThreshold = maxOf(10, (totalInteractions * 0.05).toInt())

                                if (updatedUnit.reportTokens.size >= dynamicThreshold) {
                                    // Blacklist threshold met!
                                    newlyBlacklisted.add(hash)
                                    privateDb.blacklistDao().insertAll(listOf(BlacklistEntry(hash, System.currentTimeMillis())))
                                    sharedDb.contentDao().delete(hash)
                                } else {
                                    // Just update the unit with the new reports
                                    sharedDb.contentDao().insert(updatedUnit)
                                }
                            }
                        }
                    }
                }
                
                // Update sync time
                prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()

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
            sharedDb.contentDao().delete(hash)
        }
    }
}
