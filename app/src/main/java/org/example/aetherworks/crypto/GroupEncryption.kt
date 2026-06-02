package org.example.aetherworks.crypto

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
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class WrappedKey(
    val ephemeralPublicKeyBase64: String,
    val wrappedAesKeyBase64: String,
    val ivBase64: String
)

object GroupEncryption {

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

            // 3. Derive Key Wrapping Key (KWK)
            val md = MessageDigest.getInstance("SHA-256")
            val kwk = md.digest(sharedSecret)

            // 4. Wrap AES Key using AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(kwk, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val iv = cipher.iv
            val wrapped = cipher.doFinal(aesKey)

            val wrappedObj = WrappedKey(
                ephemeralPublicKeyBase64 = Base64.encodeToString(ephemeralPub.encoded, Base64.NO_WRAP),
                wrappedAesKeyBase64 = Base64.encodeToString(wrapped, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
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

            // 2. Derive Key Wrapping Key (KWK)
            val md = MessageDigest.getInstance("SHA-256")
            val kwk = md.digest(sharedSecret)

            // 3. Unwrap AES Key
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(kwk, "AES")
            val iv = Base64.decode(wrappedObj.ivBase64, Base64.DEFAULT)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)

            val wrapped = Base64.decode(wrappedObj.wrappedAesKeyBase64, Base64.DEFAULT)
            cipher.doFinal(wrapped)
        } catch (e: Exception) {
            null
        }
    }
}
