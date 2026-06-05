package app.clearspace.network.crypto

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class WrappedKey(
    val ephemeralPublicKeyBase64: String,
    val wrappedAesKeyBase64: String,
    val ivBase64: String,
    // FIX H1: HKDF salt stored alongside the wrapped key so the receiver can derive the same KWK.
    val hkdfSaltBase64: String? = null
)

@Serializable
data class WrappedMessage(
    val wrappedKeysJson: String,
    val ivBase64: String,
    val ciphertextBase64: String
)

object GroupEncryption {

    /**
     * FIX H1: HKDF-SHA256 implementation using only standard JCE (HmacSHA256).
     * Replaces the insecure bare SHA-256(sharedSecret) previously used as the KWK.
     *
     * HKDF-Extract + HKDF-Expand per RFC 5869:
     *   PRK  = HMAC-SHA256(salt, IKM)
     *   T(1) = HMAC-SHA256(PRK, info || 0x01)
     *
     * The raw X25519 output is not uniformly distributed; HKDF provides proper key derivation
     * with domain separation via the [info] parameter.
     */
    private fun hkdfSha256(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int = 32
    ): ByteArray {
        // Extract
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(inputKeyMaterial)
        // Expand (single block T(1), sufficient for 32-byte output)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(0x01.toByte())
        return mac.doFinal().copyOf(length)
    }

    private val HKDF_INFO = "ClearSpace-GroupKWK".toByteArray(Charsets.UTF_8)

    /**
     * Wraps the given [aesKey] for multiple recipients using their X25519 public keys.
     * Returns a JSON map of: Recipient Ed25519 PublicKey -> WrappedKey JSON
     */
    fun wrapKeyForRecipients(
        aesKey: ByteArray,
        recipientPublicKeys: Map<String, String> // Map<Ed25519 Base64, X25519 Base64>
    ): String {
        val resultMap = mutableMapOf<String, String>()

        for ((ed25519Pub, x25519PubStr) in recipientPublicKeys) {
            val recipientPubBytes = Base64.decode(x25519PubStr, Base64.DEFAULT)
            val recipientPubParams = X25519PublicKeyParameters(recipientPubBytes, 0)

            // 1. Generate ephemeral key pair
            val gen = X25519KeyPairGenerator()
            gen.init(X25519KeyGenerationParameters(SecureRandom()))
            val ephemeralPair: AsymmetricCipherKeyPair = gen.generateKeyPair()
            val ephemeralPriv = ephemeralPair.private as X25519PrivateKeyParameters
            val ephemeralPub = ephemeralPair.public as X25519PublicKeyParameters

            // 2. Perform ECDH agreement
            val agreement = X25519Agreement()
            agreement.init(ephemeralPriv)
            val sharedSecret = ByteArray(agreement.agreementSize)
            agreement.calculateAgreement(recipientPubParams, sharedSecret, 0)

            // 3. FIX H1: Derive Key Wrapping Key (KWK) via HKDF-SHA256 instead of bare SHA-256
            val hkdfSalt = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val kwk = hkdfSha256(sharedSecret, hkdfSalt, HKDF_INFO)
            java.util.Arrays.fill(sharedSecret, 0)

            // 4. Wrap AES Key using AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(kwk, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val iv = cipher.iv
            val wrapped = cipher.doFinal(aesKey)
            java.util.Arrays.fill(kwk, 0)

            val wrappedObj = WrappedKey(
                ephemeralPublicKeyBase64 = Base64.encodeToString(ephemeralPub.encoded, Base64.NO_WRAP),
                wrappedAesKeyBase64 = Base64.encodeToString(wrapped, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
                hkdfSaltBase64 = Base64.encodeToString(hkdfSalt, Base64.NO_WRAP)
            )

            resultMap[ed25519Pub] = Json.encodeToString(wrappedObj)
        }

        return Json.encodeToString(resultMap)
    }

    /**
     * Unwraps the AES key for the current user.
     */
    fun unwrapKey(
        wrappedKeyJson: String,
        myPrivX25519Bytes: ByteArray
    ): ByteArray? {
        return try {
            val wrappedObj = Json.decodeFromString<WrappedKey>(wrappedKeyJson)
            val ephemeralPubBytes = Base64.decode(wrappedObj.ephemeralPublicKeyBase64, Base64.DEFAULT)
            val ephemeralPubParams = X25519PublicKeyParameters(ephemeralPubBytes, 0)
            
            val myPrivParams = X25519PrivateKeyParameters(myPrivX25519Bytes, 0)

            // 1. Perform ECDH agreement
            val agreement = X25519Agreement()
            agreement.init(myPrivParams)
            val sharedSecret = ByteArray(agreement.agreementSize)
            agreement.calculateAgreement(ephemeralPubParams, sharedSecret, 0)

            // 2. FIX H1: Derive KWK via HKDF-SHA256.
            // Backwards compat: old wrapped keys without hkdfSaltBase64 use a zero-byte salt.
            // New keys always carry a random salt.
            val hkdfSalt = wrappedObj.hkdfSaltBase64?.let { Base64.decode(it, Base64.DEFAULT) }
                ?: ByteArray(32) // Legacy fallback: zero salt
            val kwk = hkdfSha256(sharedSecret, hkdfSalt, HKDF_INFO)
            java.util.Arrays.fill(sharedSecret, 0)

            // 3. Unwrap AES Key
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(kwk, "AES")
            val iv = Base64.decode(wrappedObj.ivBase64, Base64.DEFAULT)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
            java.util.Arrays.fill(kwk, 0)

            val wrapped = Base64.decode(wrappedObj.wrappedAesKeyBase64, Base64.DEFAULT)
            cipher.doFinal(wrapped)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encrypts a payload for multiple recipients and returns a JSON serialized WrappedMessage.
     */
    fun encryptPayloadForRecipients(
        plaintextBytes: ByteArray,
        recipientPublicKeys: Map<String, String> // Map<Ed25519 Base64, X25519 Base64>
    ): String {
        val aesKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val wrappedKeysJson = wrapKeyForRecipients(aesKey, recipientPublicKeys)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(aesKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintextBytes)

        val wrappedMessage = WrappedMessage(
            wrappedKeysJson = wrappedKeysJson,
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
        return Json.encodeToString(wrappedMessage)
    }
}

