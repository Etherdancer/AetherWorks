package app.clearspace.network.moderation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import app.clearspace.network.storage.db.entity.ContentUnit

class ContentReporter(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun reportContent(contentHash: String, reason: String) {
        withContext(Dispatchers.IO) {
            try {
                // Add the report to a "reports" collection for review
                val report = hashMapOf(
                    "contentHash" to contentHash,
                    "reason" to reason,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("reports").add(report).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
