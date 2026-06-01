package org.example.aetherworks.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * GatekeeperAgent
 * Handles all authentication, key derivation (Argon2id), and Android Keystore operations.
 * Ensures the app's encrypted database key is securely wrapped and never exposed in plaintext.
 */
class GatekeeperAgent {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "AetherWorksMasterKey"
        
        // Argon2id parameters (OWASP recommended baseline for mobile)
        private const val ARGON2_ITERATIONS = 3
        private const val ARGON2_MEMORY_KB = 65536 // 64MB
        private const val ARGON2_PARALLELISM = 1
        private const val HASH_LENGTH = 32 // 256-bit key
    }

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    private val secureRandom = SecureRandom()

    init {
        ensureMasterKeyExists()
    }

    /**
     * Generates a strong master key in the Android Keystore if it doesn't already exist.
     * This key is used to encrypt the user's password-derived key or database key.
     */
    private fun ensureMasterKeyExists() {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val parameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // We do NOT require user authentication (biometrics) just yet for the base key,
                // because the user will provide a PIN/password to unwrap their data.
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getMasterKey(): SecretKey {
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Derives a cryptographic key from a user's PIN/Password using Argon2id.
     */
    fun deriveKeyFromPassword(password: CharArray, salt: ByteArray): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ARGON2_ITERATIONS)
            .withMemoryAsKB(ARGON2_MEMORY_KB)
            .withParallelism(ARGON2_PARALLELISM)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(parameters)

        // Convert char array to byte array securely
        val passwordBytes = Charsets.UTF_8.encode(java.nio.CharBuffer.wrap(password)).array()
        val result = ByteArray(HASH_LENGTH)
        
        generator.generateBytes(passwordBytes, result)
        
        // Zero out the password bytes immediately
        passwordBytes.fill(0)
        
        return result
    }

    /**
     * Generates a secure random salt for Argon2.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Encrypts a payload (like a database encryption key) using the hardware-backed Master Key.
     */
    fun wrapData(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return Pair(iv, encrypted)
    }

    /**
     * Decrypts a payload using the hardware-backed Master Key.
     */
    fun unwrapData(iv: ByteArray, encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)
        return cipher.doFinal(encryptedData)
    }
}
