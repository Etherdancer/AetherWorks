package app.clearspace.network.network

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import app.clearspace.network.crypto.KeyManager
import app.clearspace.network.storage.db.AetherDatabase

/**
 * TorMediaService
 * Handles long-running background streaming of media over Tor sockets.
 * Server mode listens on port 8081.
 * Client mode connects to an onion address via local SOCKS proxy.
 */
class TorMediaService : Service() {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeStreams = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        startServer()
    }

    private fun startServer() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            try {
                // Bind explicitly to 127.0.0.1 (localhost) to ensure the local network cannot bypass Tor
                val loopback = java.net.InetAddress.getByName("127.0.0.1")
                serverSocket = ServerSocket(8081, 50, loopback)
                Log.d("TorMedia", "Tor Media Server listening on port 8081")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    Log.d("TorMedia", "Accepted incoming media connection")
                    handleIncomingConnection(client)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("TorMedia", "Media Server Error", e)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val contentHash = intent?.getStringExtra(EXTRA_CONTENT_HASH)
        val onionAddress = intent?.getStringExtra(EXTRA_ONION_ADDRESS)

        if (contentHash != null) {
            when (action) {
                ACTION_START_STREAM -> {
                    Log.d("TorMedia", "Preparing to host media stream for hash: $contentHash")
                    activeStreams.add(contentHash)
                    // The server loop is already running and waiting on port 8081
                }
                ACTION_DOWNLOAD_STREAM -> {
                    if (onionAddress != null) {
                        Log.d("TorMedia", "Starting download stream for hash $contentHash from $onionAddress")
                        downloadMediaFromPeer(onionAddress, contentHash)
                    } else {
                        Log.e("TorMedia", "Cannot download: missing onion address")
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun handleIncomingConnection(socket: Socket) {
        scope.launch {
            try {
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val keyManager = KeyManager(this@TorMediaService)
                
                // 1. Read Auth Packet
                val authBytes = ByteArray(4096)
                val authRead = input.read(authBytes)
                if (authRead <= 0) return@launch
                
                val authStr = String(authBytes, 0, authRead)
                val authJson = JSONObject(authStr).getJSONObject("auth")
                val pubKey = authJson.getString("pubKey")
                val timestamp = authJson.getLong("timestamp")
                val signatureBase64 = authJson.getString("signature")
                
                if (Math.abs(System.currentTimeMillis() - timestamp) > 300000) {
                    socket.close(); return@launch
                }
                
                val signatureBytes = android.util.Base64.decode(signatureBase64, android.util.Base64.NO_WRAP)
                val dataToVerify = (pubKey + timestamp.toString()).toByteArray(Charsets.UTF_8)
                val pubKeyBytes = android.util.Base64.decode(pubKey, android.util.Base64.NO_WRAP)
                if (!keyManager.verifySignature(dataToVerify, signatureBytes, pubKeyBytes)) {
                    socket.close(); return@launch
                }
                
                val db = AetherDatabase.getPrivateDatabase()
                val peer = db.peerDao().getByPublicKey(pubKey)
                if (peer == null || peer.trustLevel == app.clearspace.network.storage.db.entity.TrustLevel.ACQUAINTANCE) {
                    Log.e("TorMedia", "Peer untrusted or unknown")
                    socket.close(); return@launch
                }
                
                // 2. Send Server Auth
                val myIdentity = keyManager.getOrGenerateIdentity()
                val myPubKey = android.util.Base64.encodeToString(myIdentity.second, android.util.Base64.NO_WRAP)
                val myTs = System.currentTimeMillis()
                val myData = (myPubKey + myTs.toString()).toByteArray(Charsets.UTF_8)
                val mySig = android.util.Base64.encodeToString(keyManager.signData(myData), android.util.Base64.NO_WRAP)
                
                val serverAuth = JSONObject().apply {
                    put("auth", JSONObject().apply {
                        put("pubKey", myPubKey)
                        put("timestamp", myTs)
                        put("signature", mySig)
                    })
                }
                output.write(serverAuth.toString().toByteArray(Charsets.UTF_8))
                output.flush()
                
                // 3. Read Request for Hash
                val reqBytes = ByteArray(1024)
                val reqRead = input.read(reqBytes)
                if (reqRead <= 0) return@launch
                val reqJson = JSONObject(String(reqBytes, 0, reqRead))
                val requestedHash = reqJson.getString("contentHash")
                
                if (!activeStreams.contains(requestedHash)) {
                    Log.e("TorMedia", "Requested hash not active for streaming")
                    socket.close(); return@launch
                }
                
                val content = db.contentDao().getByHashSync(requestedHash)
                val path = content?.videoPath ?: content?.imagePath
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        Log.d("TorMedia", "Streaming file ${file.name} to peer")
                        val status = JSONObject().apply { put("status", "ok"); put("size", file.length()) }
                        output.write(status.toString().toByteArray(Charsets.UTF_8))
                        output.flush()
                        
                        // Wait for ack
                        input.read()
                        
                        file.inputStream().use { fileIn ->
                            fileIn.copyTo(output)
                        }
                        Log.d("TorMedia", "Stream complete")
                    } else {
                        Log.e("TorMedia", "File not found")
                    }
                }
            } catch (e: Exception) {
                Log.e("TorMedia", "Error handling incoming stream", e)
            } finally {
                socket.close()
            }
        }
    }

    private fun downloadMediaFromPeer(onionAddress: String, contentHash: String) {
        scope.launch {
            var attempt = 0
            val maxAttempts = 3
            var connected = false
            
            while (attempt < maxAttempts && !connected) {
                try {
                    Log.d("TorMedia", "Connecting to peer $onionAddress via Tor SOCKS5... (Attempt ${attempt + 1})")
                    val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
                    val socket = Socket(proxy)
                    // Note: We map Tor external port 81 to internal 8081
                    socket.connect(InetSocketAddress(onionAddress, 81), 60000)
                    
                    val output = socket.getOutputStream()
                    val input = socket.getInputStream()
                    val keyManager = KeyManager(this@TorMediaService)
                    
                    // 1. Send Client Auth
                    val myIdentity = keyManager.getOrGenerateIdentity()
                    val myPubKey = android.util.Base64.encodeToString(myIdentity.second, android.util.Base64.NO_WRAP)
                    val myTs = System.currentTimeMillis()
                    val myData = (myPubKey + myTs.toString()).toByteArray(Charsets.UTF_8)
                    val mySig = android.util.Base64.encodeToString(keyManager.signData(myData), android.util.Base64.NO_WRAP)
                    
                    val clientAuth = JSONObject().apply {
                        put("auth", JSONObject().apply {
                            put("pubKey", myPubKey)
                            put("timestamp", myTs)
                            put("signature", mySig)
                        })
                    }
                    output.write(clientAuth.toString().toByteArray(Charsets.UTF_8))
                    output.flush()
                    
                    // 2. Wait for Server Auth
                    val authBytes = ByteArray(4096)
                    val authRead = input.read(authBytes)
                    if (authRead <= 0) { socket.close(); throw Exception("No auth response") }
                    
                    val authStr = String(authBytes, 0, authRead)
                    val serverAuthJson = JSONObject(authStr).getJSONObject("auth")
                    val serverPubKey = serverAuthJson.getString("pubKey")
                    val serverTimestamp = serverAuthJson.getLong("timestamp")
                    val serverSignature = serverAuthJson.getString("signature")
                    
                    val serverSigBytes = android.util.Base64.decode(serverSignature, android.util.Base64.NO_WRAP)
                    val serverDataToVerify = (serverPubKey + serverTimestamp.toString()).toByteArray(Charsets.UTF_8)
                    val serverPubKeyBytes = android.util.Base64.decode(serverPubKey, android.util.Base64.NO_WRAP)
                    
                    if (!keyManager.verifySignature(serverDataToVerify, serverSigBytes, serverPubKeyBytes)) {
                        socket.close(); throw Exception("Invalid server signature")
                    }
                    
                    // 3. Request Hash
                    val req = JSONObject().apply { put("contentHash", contentHash) }
                    output.write(req.toString().toByteArray(Charsets.UTF_8))
                    output.flush()
                    
                    // 4. Read Status
                    val statusBytes = ByteArray(1024)
                    val statusRead = input.read(statusBytes)
                    val statusJson = JSONObject(String(statusBytes, 0, statusRead))
                    if (statusJson.getString("status") == "ok") {
                        output.write(1) // Send ack
                        output.flush()
                        
                        val outFile = File(filesDir, "media_$contentHash")
                        outFile.outputStream().use { fileOut ->
                            input.copyTo(fileOut)
                        }
                        Log.d("TorMedia", "Successfully downloaded media: ${outFile.absolutePath}")
                        
                        // Update DB
                        val db = AetherDatabase.getPrivateDatabase()
                        val content = db.contentDao().getByHashSync(contentHash)
                        if (content != null) {
                            val updated = if (content.videoPath != null) {
                                content.copy(videoPath = outFile.absolutePath)
                            } else {
                                content.copy(imagePath = outFile.absolutePath)
                            }
                            db.contentDao().insert(updated)
                        }
                        connected = true
                    } else {
                        throw Exception("Server rejected stream")
                    }
                    socket.close()
                } catch (e: Exception) {
                    Log.e("TorMedia", "Failed to stream media", e)
                    attempt++
                    if (attempt < maxAttempts) {
                        delay(5000L * attempt) // Exponential backoff
                    }
                }
            }
            if (!connected) {
                Log.e("TorMedia", "Max retry attempts reached. Could not download media.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { serverSocket?.close() } catch (e: Exception) {}
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null 
    }

    companion object {
        const val ACTION_START_STREAM = "app.clearspace.network.ACTION_START_STREAM"
        const val ACTION_DOWNLOAD_STREAM = "app.clearspace.network.ACTION_DOWNLOAD_STREAM"
        const val EXTRA_CONTENT_HASH = "EXTRA_CONTENT_HASH"
        const val EXTRA_ONION_ADDRESS = "EXTRA_ONION_ADDRESS"
    }
}
