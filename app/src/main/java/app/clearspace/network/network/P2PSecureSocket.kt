package app.clearspace.network.network

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
        
        val tm = trustManager ?: throw IllegalArgumentException("TrustManager must be explicitly provided to prevent Trust-All vulnerabilities.")

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
