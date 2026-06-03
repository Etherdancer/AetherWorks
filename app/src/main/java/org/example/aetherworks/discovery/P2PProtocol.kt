package org.example.aetherworks.discovery

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.persona.PersonaAgent
import org.example.aetherworks.security.guard.SecureP2PManager
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.RelayPacket
import org.example.aetherworks.storage.db.entity.Visibility
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@Serializable
data class ContentHeader(
    val contentHash: String,
    val title: String,
    val authorAlias: String,
    val timestamp: Long,
    val thumbnailBase64: String?,
    val categoryFlags: String,
    val emotionFlags: String,
    val reputationScore: Int
)

class P2PServer(
    private val context: Context,
    private val db: AetherDatabase
) {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newFixedThreadPool(10) // Limit concurrency to prevent DoS
    private val activeConnections = java.util.concurrent.ConcurrentHashMap<String, Int>()
    
    companion object {
        private const val MAX_CONNECTIONS_PER_IP = 3
    }
    
    var localPort: Int = 0
        private set

    fun start(): Int {
        if (isRunning.get()) return localPort
        
        serverSocket = SecureP2PManager.getServerSocketFactory().createServerSocket(0)
        localPort = serverSocket!!.localPort
        isRunning.set(true)

        thread(start = true, name = "P2PServer-Thread") {
            try {
                while (isRunning.get()) {
                    val client = serverSocket?.accept() ?: break
                    client.soTimeout = 3000 // Prevent connection hanging DoS
                    executor.submit { handleClient(client) }
                }
            } catch (e: Exception) {
                // Socket closed or error
            }
        }
        return localPort
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        executor.shutdownNow()
        activeConnections.clear()
    }

    private fun handleClient(socket: Socket) {
        val ip = socket.inetAddress?.hostAddress ?: "unknown"
        val currentCount = activeConnections.getOrDefault(ip, 0)
        if (currentCount >= MAX_CONNECTIONS_PER_IP) {
            try { socket.close() } catch (e: Exception) {}
            return
        }
        activeConnections[ip] = currentCount + 1

        try {
            socket.use { s ->
                val reader = InputStreamReader(s.getInputStream())
                val writer = PrintWriter(s.getOutputStream(), true)

                val request = readBoundedLine(s.getInputStream(), 4096) ?: return@use
                val parts = request.split(" ")
                var command = parts[0]

                if (command.endsWith("_v1")) {
                    command = command.removeSuffix("_v1")
                }

                val timestampStr = parts.lastOrNull()
                val timestamp = timestampStr?.toLongOrNull()
                if (timestamp == null || Math.abs(System.currentTimeMillis() - timestamp) > 300000) {
                    writer.println("ERROR: INVALID_TIMESTAMP_OR_EXPIRED")
                    return@use
                }

                when (command) {
                    "INDEX" -> {
                        // Return all public headers
                        val publicContent = db.contentDao().getByVisibilitySync(Visibility.PUBLIC)
                        val headers = publicContent.map { unit ->
                            ContentHeader(
                                contentHash = unit.contentHash,
                                title = unit.title,
                                authorAlias = unit.authorAlias,
                                timestamp = unit.timestamp,
                                thumbnailBase64 = unit.thumbnailBase64,
                                categoryFlags = unit.categoryFlags,
                                emotionFlags = unit.emotionFlags,
                                reputationScore = unit.likeTokens.size - unit.dislikeTokens.size
                            )
                        }
                        writer.println(Json.encodeToString(headers))
                    }
                    "GET" -> {
                        if (parts.size > 1) {
                            val hash = parts[1]
                            val unit = db.contentDao().getByHashSync(hash)
                            if (unit != null && unit.visibility == Visibility.PUBLIC) {
                                writer.println(Json.encodeToString(unit))
                            } else {
                                writer.println("ERROR: NOT_FOUND_OR_PRIVATE")
                            }
                        }
                    }
                    "PROFILE" -> {
                        val personaAgent = PersonaAgent(context, KeyManager(context))
                        if (personaAgent.showProfileToNearbyUsers) {
                            val profile = personaAgent.getProfile()
                            if (profile != null) {
                                writer.println(Json.encodeToString(profile))
                            } else {
                                writer.println("ERROR: NO_PROFILE")
                            }
                        } else {
                            writer.println("ERROR: PROFILE_PRIVATE")
                        }
                    }
                    "RELAY_INDEX" -> {
                        val currentTime = System.currentTimeMillis()
                        val packets = kotlinx.coroutines.runBlocking {
                            db.relayPacketDao().getValidRelayPackets(currentTime)
                        }
                        val packetIds = packets.map { it.packetId }
                        writer.println(Json.encodeToString(packetIds))
                    }
                    "GET_RELAY" -> {
                        if (parts.size > 1) {
                            val packetId = parts[1]
                            val currentTime = System.currentTimeMillis()
                            val packet = kotlinx.coroutines.runBlocking {
                                db.relayPacketDao().getValidRelayPackets(currentTime).find { it.packetId == packetId }
                            }
                            if (packet != null) {
                                writer.println(Json.encodeToString(packet))
                            } else {
                                writer.println("ERROR: NOT_FOUND")
                            }
                        }
                    }
                    else -> {
                        writer.println("ERROR: UNKNOWN_COMMAND")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            val newCount = activeConnections.getOrDefault(ip, 1) - 1
            if (newCount <= 0) {
                activeConnections.remove(ip)
            } else {
                activeConnections[ip] = newCount
            }
            try { socket.close() } catch (e: Exception) {}
        }
    }

    // Prevents memory exhaustion attacks (infinite strings without newlines)
    private fun readBoundedLine(input: InputStream, maxLength: Int): String? {
        val builder = java.lang.StringBuilder()
        var count = 0
        while (count < maxLength) {
            val byte = input.read()
            if (byte == -1) break
            val char = byte.toChar()
            if (char == '\n') break
            if (char != '\r') {
                builder.append(char)
                count++
            }
        }
        if (count >= maxLength) return null // Drop connection if limit exceeded
        return if (builder.isEmpty()) null else builder.toString()
    }
}

object P2PClient {
    // Shared read bounded buffer logic (5 MB limit for large responses)
    private fun readBoundedResponse(input: InputStream, maxLength: Int = 5_000_000): String? {
        val builder = java.lang.StringBuilder()
        var count = 0
        while (count < maxLength) {
            val byte = input.read()
            if (byte == -1) break
            val char = byte.toChar()
            if (char == '\n') break
            if (char != '\r') {
                builder.append(char)
                count++
            }
        }
        if (count >= maxLength) return null // Protection against memory DoS
        return if (builder.isEmpty()) null else builder.toString()
    }

    suspend fun fetchIndex(ip: String, port: Int): List<ContentHeader>? = withContext(Dispatchers.IO) {
        try {
            SecureP2PManager.getSocketFactory().createSocket(ip, port).use { socket ->
                socket.soTimeout = 5000
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println("INDEX_v1 ${System.currentTimeMillis()}")
                
                val response = readBoundedResponse(socket.getInputStream())
                if (response != null && response.startsWith("[")) {
                    return@withContext Json.decodeFromString<List<ContentHeader>>(response)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun fetchContent(ip: String, port: Int, hash: String): ContentUnit? = withContext(Dispatchers.IO) {
        try {
            SecureP2PManager.getSocketFactory().createSocket(ip, port).use { socket ->
                socket.soTimeout = 10000
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println("GET_v1 $hash ${System.currentTimeMillis()}")
                
                val response = readBoundedResponse(socket.getInputStream())
                if (response != null && response.startsWith("{")) {
                    val unit = Json.decodeFromString<ContentUnit>(response)
                    
                    // Verify data integrity against spoofing/MITM injection
                    val dataToHash = unit.title + unit.body
                    val md = MessageDigest.getInstance("SHA-256")
                    val computedHashBytes = md.digest(dataToHash.toByteArray(Charsets.UTF_8))
                    val computedHash = computedHashBytes.joinToString("") { "%02x".format(it) }
                    
                    if (computedHash == unit.contentHash && computedHash == hash) {
                        if (!org.example.aetherworks.reputation.ReputationAgent.verifyProofOfWork(unit.contentHash, unit.powNonce)) {
                            System.err.println("PoW mismatch. Rejecting ContentUnit from peer.")
                            return@withContext null
                        }
                        return@withContext unit
                    } else {
                        System.err.println("Hash mismatch. Rejecting ContentUnit from peer.")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun fetchProfile(ip: String, port: Int): org.example.aetherworks.persona.Profile? = withContext(Dispatchers.IO) {
        try {
            SecureP2PManager.getSocketFactory().createSocket(ip, port).use { socket ->
                socket.soTimeout = 5000
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println("PROFILE_v1 ${System.currentTimeMillis()}")
                
                val response = readBoundedResponse(socket.getInputStream(), 1024 * 500) // 500 KB limit for profile
                if (response != null && response.startsWith("{")) {
                    return@withContext Json.decodeFromString<org.example.aetherworks.persona.Profile>(response)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun fetchRelayIndex(ip: String, port: Int): List<String>? = withContext(Dispatchers.IO) {
        try {
            SecureP2PManager.getSocketFactory().createSocket(ip, port).use { socket ->
                socket.soTimeout = 5000
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println("RELAY_INDEX_v1 ${System.currentTimeMillis()}")
                
                val response = readBoundedResponse(socket.getInputStream(), 1024 * 1024)
                if (response != null && response.startsWith("[")) {
                    return@withContext Json.decodeFromString<List<String>>(response)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun fetchRelayPacket(ip: String, port: Int, packetId: String): RelayPacket? = withContext(Dispatchers.IO) {
        try {
            SecureP2PManager.getSocketFactory().createSocket(ip, port).use { socket ->
                socket.soTimeout = 10000
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println("GET_RELAY_v1 $packetId ${System.currentTimeMillis()}")
                
                val response = readBoundedResponse(socket.getInputStream(), 5_000_000)
                if (response != null && response.startsWith("{")) {
                    return@withContext Json.decodeFromString<RelayPacket>(response)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
