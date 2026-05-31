package org.example.aetherworks.reputation

import android.util.Base64
import org.example.aetherworks.crypto.KeyManager
import java.security.MessageDigest

/**
 * Decentralized Anonymous Rating System
 * Tracks reputation through community feedback without identifying voters.
 */
class ReputationAgent(private val keyManager: KeyManager) {

    // Lazy-loaded persistent secure random secret used to generate anonymous tokens.
    // It never leaves the device.
    private val voterSecretBase64: String by lazy {
        Base64.encodeToString(keyManager.getOrGenerateVoterSecret(), Base64.NO_WRAP)
    }

    /**
     * Generates a deterministic, anonymous LIKE token for a specific content item.
     * Formula: SHA-256(contentHash + voterSecret + LIKE)
     */
    fun generateLikeToken(contentHash: String): String {
        return hash("$contentHash:$voterSecretBase64:LIKE")
    }

    /**
     * Generates a deterministic, anonymous DISLIKE token for a specific content item.
     * Formula: SHA-256(contentHash + voterSecret + DISLIKE)
     */
    fun generateDislikeToken(contentHash: String): String {
        return hash("$contentHash:$voterSecretBase64:DISLIKE")
    }

    /**
     * Generates a deterministic, anonymous FLAG token (for categories or emotions).
     * Formula: SHA-256(contentHash + voterSecret + FLAG + flagName)
     */
    fun generateFlagToken(contentHash: String, flagName: String): String {
        return hash("$contentHash:$voterSecretBase64:FLAG:$flagName")
    }

    /**
     * Merges reputation token sets when discovering the same content from multiple peers.
     * Using Set Union mathematically prevents double-counting.
     */
    fun mergeReputation(localTokens: Set<String>, remoteTokens: Set<String>): Set<String> {
        val union = localTokens.union(remoteTokens)
        
        // Anti-Exploitation: Token Cardinality Caps to prevent storage exhaustion attacks
        if (union.size > 10_000) {
            return union.take(10_000).toSet()
        }
        return union
    }

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
