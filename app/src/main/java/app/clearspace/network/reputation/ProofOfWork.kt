package app.clearspace.network.reputation

import java.security.MessageDigest

object ProofOfWork {

    /**
     * Generates a nonce such that SHA-256(data + nonce) has at least `difficulty` leading zero bytes.
     * To prevent battery drain on mobile, difficulty is usually kept very low (e.g. 1 or 2).
     */
    fun generate(data: String, difficulty: Int = 1): Long {
        val digest = MessageDigest.getInstance("SHA-256")
        var nonce = 0L
        
        while (true) {
            val input = "$data:$nonce"
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            
            if (checkDifficulty(hashBytes, difficulty)) {
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
        
        return checkDifficulty(hashBytes, difficulty)
    }

    private fun checkDifficulty(hash: ByteArray, difficulty: Int): Boolean {
        for (i in 0 until difficulty) {
            if (hash[i] != 0.toByte()) return false
        }
        return true
    }
}
