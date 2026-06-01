package org.example.aetherworks.reputation

import java.security.MessageDigest

object ProofOfWork {

    /**
     * Generates a nonce such that SHA-256(data + nonce) has at least `difficulty` leading zero hex characters.
     * To prevent battery drain on mobile, difficulty is usually kept very low (e.g. 1 or 2).
     */
    fun generate(data: String, difficulty: Int = 1): Long {
        val digest = MessageDigest.getInstance("SHA-256")
        val prefix = "0".repeat(difficulty)
        var nonce = 0L
        
        while (true) {
            val input = "$data:$nonce"
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }
            
            if (hexString.startsWith(prefix)) {
                return nonce
            }
            nonce++
        }
    }

    /**
     * Verifies if a given nonce produces a valid Proof of Work for the provided data and difficulty.
     */
    fun verify(data: String, nonce: Long, difficulty: Int = 1): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$data:$nonce"
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        val prefix = "0".repeat(difficulty)
        
        return hexString.startsWith(prefix)
    }
}
