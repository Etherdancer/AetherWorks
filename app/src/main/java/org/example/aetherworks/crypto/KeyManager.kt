package org.example.aetherworks.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyManager(context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "aether_master_key"
        private const val PREFS_NAME = "aether_crypto_prefs"
        private const val KEY_DB_SECRET = "db_secret_encrypted"
        private const val KEY_ED25519_PRIV = "ed25519_priv_encrypted"
        private const val KEY_ED25519_PUB = "ed25519_pub"
        private const val KEY_X25519_PRIV = "x25519_priv_encrypted"
        private const val KEY_X25519_PUB = "x25519_pub"
        private const val KEY_VOTER_SECRET = "voter_secret_encrypted"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val builder = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            
            // Try StrongBox on Android P+
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    builder.setIsStrongBoxBacked(true)
                    keyGenerator.init(builder.build())
                    keyGenerator.generateKey()
                    return
                }
            } catch (e: Exception) {
                // Fallback to TEE
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    builder.setIsStrongBoxBacked(false)
                }
            }
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
    }

    private fun getMasterKey(): SecretKey {
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }

    private fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    private fun decryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)
        return cipher.doFinal(encrypted)
    }

    private fun getDbSecret(): ByteArray {
        val encryptedStr = prefs.getString(KEY_DB_SECRET, null)
        if (encryptedStr != null) {
            return decryptData(Base64.decode(encryptedStr, Base64.DEFAULT))
        }
        val secret = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val enc = encryptData(secret)
        prefs.edit().putString(KEY_DB_SECRET, Base64.encodeToString(enc, Base64.DEFAULT)).apply()
        return secret
    }

    /**
     * Derives the SQLCipher key by XORing the Argon2id hash with a hardware-protected random secret.
     */
    fun deriveSqlCipherKey(argon2Hash: ByteArray): ByteArray {
        require(argon2Hash.size == 32) { "Argon2 hash must be 32 bytes" }
        val dbSecret = getDbSecret()
        val result = ByteArray(32)
        for (i in 0 until 32) {
            result[i] = (argon2Hash[i].toInt() xor dbSecret[i].toInt()).toByte()
        }
        java.util.Arrays.fill(dbSecret, 0.toByte())
        return result
    }

    fun getOrGenerateIdentity(): Pair<ByteArray, ByteArray> {
        val privEncStr = prefs.getString(KEY_ED25519_PRIV, null)
        val pubStr = prefs.getString(KEY_ED25519_PUB, null)
        if (privEncStr != null && pubStr != null) {
            val priv = decryptData(Base64.decode(privEncStr, Base64.DEFAULT))
            val pubBytes = Base64.decode(pubStr, Base64.DEFAULT)
            return Pair(priv, pubBytes)
        }
        
        val gen = Ed25519KeyPairGenerator()
        gen.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val pair: AsymmetricCipherKeyPair = gen.generateKeyPair()
        val privParams = pair.private as Ed25519PrivateKeyParameters
        val pubParams = pair.public as Ed25519PublicKeyParameters
        
        val privBytes = privParams.encoded
        val pubBytes = pubParams.encoded
        
        val encPriv = encryptData(privBytes)
        prefs.edit()
            .putString(KEY_ED25519_PRIV, Base64.encodeToString(encPriv, Base64.DEFAULT))
            .putString(KEY_ED25519_PUB, Base64.encodeToString(pubBytes, Base64.DEFAULT))
            .apply()
            
        return Pair(privBytes, pubBytes)
    }

    fun getOrGenerateEncryptionIdentity(): Pair<ByteArray, ByteArray> {
        val privEncStr = prefs.getString(KEY_X25519_PRIV, null)
        val pubStr = prefs.getString(KEY_X25519_PUB, null)
        if (privEncStr != null && pubStr != null) {
            val priv = decryptData(Base64.decode(privEncStr, Base64.DEFAULT))
            val pubBytes = Base64.decode(pubStr, Base64.DEFAULT)
            return Pair(priv, pubBytes)
        }
        
        val gen = X25519KeyPairGenerator()
        gen.init(X25519KeyGenerationParameters(SecureRandom()))
        val pair: AsymmetricCipherKeyPair = gen.generateKeyPair()
        val privParams = pair.private as X25519PrivateKeyParameters
        val pubParams = pair.public as X25519PublicKeyParameters
        
        val privBytes = privParams.encoded
        val pubBytes = pubParams.encoded
        
        val encPriv = encryptData(privBytes)
        prefs.edit()
            .putString(KEY_X25519_PRIV, Base64.encodeToString(encPriv, Base64.DEFAULT))
            .putString(KEY_X25519_PUB, Base64.encodeToString(pubBytes, Base64.DEFAULT))
            .apply()
            
        return Pair(privBytes, pubBytes)
    }

    fun storePasswordHash(hash: ByteArray) {
        val enc = encryptData(hash)
        prefs.edit().putString("password_hash_encrypted", Base64.encodeToString(enc, Base64.DEFAULT)).apply()
    }

    fun getStoredPasswordHash(): ByteArray? {
        val encStr = prefs.getString("password_hash_encrypted", null) ?: return null
        return decryptData(Base64.decode(encStr, Base64.DEFAULT))
    }

    fun getOrGenerateVoterSecret(): ByteArray {
        val encStr = prefs.getString(KEY_VOTER_SECRET, null)
        if (encStr != null) {
            return decryptData(Base64.decode(encStr, Base64.DEFAULT))
        }
        val secret = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val encSecret = encryptData(secret)
        prefs.edit().putString(KEY_VOTER_SECRET, Base64.encodeToString(encSecret, Base64.DEFAULT)).apply()
        return secret
    }

    fun wipeAllKeys() {
        try {
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            // Ignore
        }
        prefs.edit().clear().apply()
    }
}
