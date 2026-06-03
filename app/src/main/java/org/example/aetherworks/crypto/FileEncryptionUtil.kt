package org.example.aetherworks.crypto

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object FileEncryptionUtil {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val ITERATIONS = 100000
    private const val KEY_LENGTH = 256

    fun encrypt(inputStream: InputStream, outputStream: OutputStream, passphrase: CharArray) {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTE).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTE).apply { secureRandom.nextBytes(this) }

        val secretKey = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        }

        outputStream.write(salt)
        outputStream.write(iv)

        CipherOutputStream(outputStream, cipher).use { cipherOut ->
            inputStream.copyTo(cipherOut)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
