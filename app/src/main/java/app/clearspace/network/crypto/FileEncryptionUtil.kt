package app.clearspace.network.crypto

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * FIX M2: Replaced PBKDF2WithHmacSHA256 key derivation with Argon2id to match the
 * rest of the app and provide significantly stronger resistance against GPU/ASIC attacks.
 *
 * Argon2id parameters (3 iterations, 64 MB memory, parallelism 1) are the same as
 * those used in GatekeeperRepository for password authentication.
 */
object FileEncryptionUtil {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16

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

    /**
     * FIX M2: Argon2id key derivation replacing PBKDF2.
     * Parameters: 3 iterations, 64 MB memory, 1 parallelism lane — consistent with GatekeeperRepository.
     */
    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(3)
            .withMemoryAsKB(65536) // 64 MB
            .withParallelism(1)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(parameters)

        // Convert passphrase CharArray to UTF-8 bytes without going through String
        val passwordBytes = charArrayToUtf8Bytes(passphrase)
        val keyBytes = ByteArray(32) // 256-bit key
        generator.generateBytes(passwordBytes, keyBytes)
        Arrays.fill(passwordBytes, 0)

        return SecretKeySpec(keyBytes, "AES")
    }

    /** Converts a CharArray to its UTF-8 byte representation without creating a String. */
    private fun charArrayToUtf8Bytes(chars: CharArray): ByteArray {
        val charset = Charsets.UTF_8
        val encoded = charset.encode(java.nio.CharBuffer.wrap(chars))
        val bytes = ByteArray(encoded.remaining())
        encoded.get(bytes)
        return bytes
    }
}
