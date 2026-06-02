package org.example.aetherworks.security.guard

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

object SecureP2PManager {

    private const val TLS_KEY_ALIAS = "aether_p2p_tls_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    private val sslContext: SSLContext by lazy {
        initSSLContext()
    }

    private fun initSSLContext(): SSLContext {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        if (!keyStore.containsAlias(TLS_KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            val parameterSpec = KeyGenParameterSpec.Builder(
                TLS_KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setCertificateSubject(X500Principal("CN=AetherWorks_P2P"))
                .setCertificateSerialNumber(BigInteger.valueOf(1))
                .setCertificateNotBefore(Date())
                .setCertificateNotAfter(Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000))
                .build()
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        }

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        // In AndroidKeyStore, passing null as the password gets the key
        keyManagerFactory.init(keyStore, null)

        // Opportunistic encryption: Trust all incoming self-signed certificates for P2P
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val context = SSLContext.getInstance("TLSv1.3")
        context.init(keyManagerFactory.keyManagers, trustAllCerts, SecureRandom())
        return context
    }

    fun getServerSocketFactory(): SSLServerSocketFactory {
        return sslContext.serverSocketFactory
    }

    fun getSocketFactory(): SSLSocketFactory {
        return sslContext.socketFactory
    }
}
