package org.example.aetherworks.security.guard

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.CertificateException
import javax.security.auth.x500.X500Principal
import android.content.Context
import android.content.SharedPreferences
import java.net.Socket
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.SSLEngine
import java.security.MessageDigest
import android.util.Base64
object SecureP2PManager {

    private const val TLS_KEY_ALIAS = "aether_p2p_tls_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    private var sslContext: SSLContext? = null
    
    fun init(context: Context) {
        if (sslContext == null) {
            sslContext = initSSLContext(context)
        }
    }

    private fun initSSLContext(context: Context): SSLContext {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        if (!keyStore.containsAlias(TLS_KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            val randomSerial = BigInteger(64, SecureRandom())
            val randomId = UUID.randomUUID().toString().take(8)
            val parameterSpec = KeyGenParameterSpec.Builder(
                TLS_KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setCertificateSubject(X500Principal("CN=AetherNode_$randomId"))
                .setCertificateSerialNumber(randomSerial)
                .setCertificateNotBefore(Date())
                .setCertificateNotAfter(Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000))
                .build()
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        }

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        // In AndroidKeyStore, passing null as the password gets the key
        keyManagerFactory.init(keyStore, null)

        val prefs = context.getSharedPreferences("tofu_certs", Context.MODE_PRIVATE)
        val trustAllCerts = arrayOf<TrustManager>(TofuTrustManager(prefs))

        val context = SSLContext.getInstance("TLSv1.3")
        context.init(keyManagerFactory.keyManagers, trustAllCerts, SecureRandom())
        return context
    }

    fun getServerSocketFactory(): SSLServerSocketFactory {
        return sslContext!!.serverSocketFactory
    }

    fun getSocketFactory(): SSLSocketFactory {
        return sslContext!!.socketFactory
    }
}

class TofuTrustManager(private val prefs: SharedPreferences) : X509ExtendedTrustManager() {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        checkTrusted(chain, null)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        checkTrusted(chain, null)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {
        checkTrusted(chain, socket)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {
        checkTrusted(chain, socket)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {
        checkTrusted(chain, null)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {
        checkTrusted(chain, null)
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

    private fun checkTrusted(chain: Array<out X509Certificate>?, socket: Socket?) {
        val cert = chain?.get(0) ?: throw CertificateException("No certificate provided")
        val ip = socket?.inetAddress?.hostAddress ?: return // Accept if no IP available for pinning
        
        val md = MessageDigest.getInstance("SHA-256")
        val fingerprint = Base64.encodeToString(md.digest(cert.encoded), Base64.NO_WRAP)
        
        val knownFingerprint = prefs.getString("cert_$ip", null)
        if (knownFingerprint == null) {
            // Trust On First Use
            prefs.edit().putString("cert_$ip", fingerprint).apply()
        } else if (knownFingerprint != fingerprint) {
            throw CertificateException("Certificate for $ip has changed! Possible MITM.")
        }
    }
}
