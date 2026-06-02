package org.example.aetherworks.reputation

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.storage.db.entity.ContentUnit

class ReputationAgent(context: Context, private val keyManager: KeyManager) {

    private val prefs: SharedPreferences = context.getSharedPreferences("reputation_prefs", Context.MODE_PRIVATE)

    companion object {
        const val POW_DIFFICULTY = 1 // Number of leading zero bytes required
        const val MAX_VOTES_PER_HOUR = 50

        /**
         * Proof of work generator. Finds a nonce that satisfies the difficulty.
         */
        fun generateProofOfWork(data: String): Long {
            var nonce = 0L
            val dataBytes = data.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256")
            
            while (true) {
                digest.reset()
                digest.update(dataBytes)
                digest.update(nonce.toString().toByteArray())
                val hash = digest.digest()
                if (checkDifficulty(hash, POW_DIFFICULTY)) {
                    return nonce
                }
                nonce++
            }
        }

        /**
         * Verifies the proof of work.
         */
        fun verifyProofOfWork(data: String, nonce: Long): Boolean {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(data.toByteArray())
            digest.update(nonce.toString().toByteArray())
            val hash = digest.digest()
            return checkDifficulty(hash, POW_DIFFICULTY)
        }

        private fun checkDifficulty(hash: ByteArray, difficulty: Int): Boolean {
            for (i in 0 until difficulty) {
                if (hash[i] != 0.toByte()) return false
            }
            return true
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
        
        // Cap cardinality at 10,000 to prevent memory exhaustion attacks
        if (mergedLikes.size > 10000) {
            val toDrop = mergedLikes.size - 10000
            mergedLikes.removeAll(mergedLikes.take(toDrop).toSet())
        }
        if (mergedDislikes.size > 10000) {
            val toDrop = mergedDislikes.size - 10000
            mergedDislikes.removeAll(mergedDislikes.take(toDrop).toSet())
        }

        return local.copy(
            likeTokens = mergedLikes,
            dislikeTokens = mergedDislikes,
            categoryTokens = (local.categoryTokens + received.categoryTokens).toMutableMap(),
            emotionTokens = (local.emotionTokens + received.emotionTokens).toMutableMap()
        )
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
