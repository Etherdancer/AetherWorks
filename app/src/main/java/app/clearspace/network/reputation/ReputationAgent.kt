package app.clearspace.network.reputation

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import app.clearspace.network.crypto.KeyManager
import app.clearspace.network.storage.db.entity.ContentUnit

class ReputationAgent(context: Context, private val keyManager: KeyManager) {

    private val prefs: SharedPreferences = context.getSharedPreferences("reputation_prefs", Context.MODE_PRIVATE)

    companion object {
        const val POW_DIFFICULTY = 1 // Number of leading zero bytes required
        const val MAX_VOTES_PER_HOUR = 50

        /**
         * Proof of work generator. Finds a nonce that satisfies the difficulty.
         */
        fun generateProofOfWork(data: String): Long {
            return ProofOfWork.generate(data, POW_DIFFICULTY)
        }

        /**
         * Verifies the proof of work.
         */
        fun verifyProofOfWork(data: String, nonce: Long): Boolean {
            return ProofOfWork.verify(data, nonce, POW_DIFFICULTY)
        }
    }

    /**
     * Generates a unique, non-reversible vote token for the given content hash.
     * Prevents duplicate votes from the same device mathematically.
     */
    fun generateVoteToken(contentHash: String): String {
        val voterSecret = keyManager.getOrGenerateVoterSecret()
        return sha256Hex(contentHash.toByteArray() + voterSecret)
    }

    /**
     * Generates a token for a category or emotion flag.
     */
    fun generateFlagToken(contentHash: String, flagName: String): String {
        val voterSecret = keyManager.getOrGenerateVoterSecret()
        return sha256Hex(contentHash.toByteArray() + flagName.toByteArray() + voterSecret)
    }

    /**
     * Checks if the user has exceeded their local rate limit for voting.
     */
    fun canVote(): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - (60 * 60 * 1000)
        
        // Cleanup old votes
        val currentVotes = prefs.getStringSet("vote_timestamps", emptySet())?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        val recentVotes = currentVotes.filter { it > windowStart }
        
        if (recentVotes.size >= MAX_VOTES_PER_HOUR) {
            return false
        }
        
        // Record new vote timestamp
        val updatedVotes = recentVotes + now
        prefs.edit().putStringSet("vote_timestamps", updatedVotes.map { it.toString() }.toSet()).apply()
        return true
    }

    /**
     * Unions token sets from a received content unit into the local content unit.
     */
    fun mergeReputation(local: ContentUnit, received: ContentUnit): ContentUnit {
        val mergedLikes = (local.likeTokens + received.likeTokens).toMutableSet()
        val mergedDislikes = (local.dislikeTokens + received.dislikeTokens).toMutableSet()
        val mergedReports = (local.reportTokens + received.reportTokens).toMutableSet()
        
        // Cap cardinality at 10,000 to prevent memory exhaustion attacks
        if (mergedLikes.size > 10000) {
            val toDrop = mergedLikes.size - 10000
            mergedLikes.removeAll(mergedLikes.take(toDrop).toSet())
        }
        if (mergedDislikes.size > 10000) {
            val toDrop = mergedDislikes.size - 10000
            mergedDislikes.removeAll(mergedDislikes.take(toDrop).toSet())
        }
        if (mergedReports.size > 10000) {
            val toDrop = mergedReports.size - 10000
            mergedReports.removeAll(mergedReports.take(toDrop).toSet())
        }

        return local.copy(
            likeTokens = mergedLikes,
            dislikeTokens = mergedDislikes,
            reportTokens = mergedReports,
            categoryTokens = (local.categoryTokens + received.categoryTokens).toMutableMap(),
            emotionTokens = (local.emotionTokens + received.emotionTokens).toMutableMap()
        )
    }

    /**
     * Generates a cryptographic report token to mark a post for moderation.
     */
    fun generateReportToken(contentHash: String): String {
        val voterSecret = keyManager.getOrGenerateVoterSecret()
        return sha256Hex(contentHash.toByteArray() + "REPORT".toByteArray() + voterSecret)
    }

    /**
     * Submits a report token to Firebase's global blacklist.
     * The server (Cloud Function) is responsible for evaluating the threshold.
     */
    fun submitReport(contentHash: String) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val reportToken = generateReportToken(contentHash)
            db.collection("global_blacklist").document(contentHash)
              .set(
                  hashMapOf("hash" to contentHash, "reportToken" to reportToken), 
                  com.google.firebase.firestore.SetOptions.merge()
              )
        } catch (e: Exception) {
            // Fail silently
        }
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
