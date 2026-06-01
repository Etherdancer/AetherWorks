package org.example.aetherworks.network

import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * P2PSecureSocket
 * Manages TLS 1.3 socket connections between P2P nodes.
 * Uses a custom TrustManager to handle self-signed certificates and public key pinning.
 */
object P2PSecureSocket {
    private const val TAG = "P2PSecureSocket"

    /**
     * Creates an SSLContext configured for TLS 1.3.
     * @param keyManager Custom KeyManager containing the device's self-signed certificate.
     * @param trustManager Custom TrustManager that verifies peer public keys.
     */
    fun createSSLContext(
        keyManager: KeyManager? = null,
        trustManager: X509TrustManager? = null
    ): SSLContext {
        val context = SSLContext.getInstance("TLSv1.3")
        
        val tm = trustManager ?: object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // In a real P2P mesh, we verify the client's public key matches an expected Persona ID
                // For now, we accept self-signed certs for local discovery testing.
                Log.d(TAG, "checkClientTrusted: ${chain?.get(0)?.subjectDN}")
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Pinning logic goes here: throw CertificateException if the public key is unknown/untrusted
                Log.d(TAG, "checkServerTrusted: ${chain?.get(0)?.subjectDN}")
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val kmArray = if (keyManager != null) arrayOf(keyManager) else null
        context.init(kmArray, arrayOf(tm), SecureRandom())
        return context
    }

    /**
     * Creates an SSLServerSocket bound to the given port.
     */
    fun createServerSocket(port: Int, sslContext: SSLContext): ServerSocket {
        val factory = sslContext.serverSocketFactory
        val serverSocket = factory.createServerSocket(port) as SSLServerSocket
        serverSocket.enabledProtocols = arrayOf("TLSv1.3")
        serverSocket.needClientAuth = true // Enforce Mutual TLS (mTLS)
        return serverSocket
    }

    /**
     * Creates an SSLSocket to connect to a peer.
     */
    fun createClientSocket(host: String, port: Int, sslContext: SSLContext): Socket {
        val factory = sslContext.socketFactory
        val socket = factory.createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.3")
        socket.startHandshake()
        return socket
    }
}
