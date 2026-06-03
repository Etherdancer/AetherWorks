package org.example.aetherworks.storage

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.aetherworks.storage.db.AetherDatabase
import java.io.File

object StorageQuotaManager {

    suspend fun enforcePublicQuota(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("aether_settings", Context.MODE_PRIVATE)
        val quotaMb = prefs.getInt("public_library_quota_mb", 500)
        
        if (quotaMb < 0) return@withContext // Unlimited
        
        val maxBytes = quotaMb * 1024L * 1024L
        val dbFile = context.getDatabasePath("aether_shared.db")
        
        if (!dbFile.exists()) return@withContext
        
        val currentSize = dbFile.length()
        
        if (currentSize > maxBytes) {
            val db = AetherDatabase.getSharedDatabase()
            val dao = db.contentDao()
            
            val targetSize = maxBytes * 0.9
            var deletedSomething = false
            
            // Delete in a single batch to avoid infinite loops if VACUUM isn't shrinking enough
            val overageBytes = currentSize - targetSize
            val itemsToDeleteEstimate = (overageBytes / 50000).toInt().coerceAtLeast(10).coerceAtMost(50) // Assuming avg item is 50KB
            
            val lowestItems = dao.getLowestReputationPublicContent(itemsToDeleteEstimate)
            
            for (item in lowestItems) {
                dao.delete(item.contentHash)
                
                // Attempt to delete associated local media files if any exist
                item.imagePath?.let { 
                    val f = File(it)
                    if (f.exists()) f.delete()
                }
                item.videoPath?.let { 
                    val f = File(it)
                    if (f.exists()) f.delete()
                }
                
                deletedSomething = true
            }
            
            if (deletedSomething) {
                try {
                    db.query(SimpleSQLiteQuery("VACUUM"))
                } catch (e: Exception) {
                    // Ignore vacuum errors
                }
            }
        }
    }
}
