package org.example.aetherworks.discovery

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.aetherworks.MainActivity
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.persona.PersonaAgent
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.Visibility
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
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
    var localPort: Int = 0
        private set

    fun start(): Int {
        if (isRunning.get()) return localPort
        
        serverSocket = ServerSocket(0)
        localPort = serverSocket!!.localPort
        isRunning.set(true)

        thread(start = true, name = "P2PServer-Thread") {
            try {
                while (isRunning.get()) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
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
    }

    private fun handleClient(socket: Socket) {
        thread(start = true) {
            try {
                socket.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    val writer = PrintWriter(s.getOutputStream(), true)

                    val request = reader.readLine() ?: return@use
                    val parts = request.split(" ")
                    val command = parts[0]

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
                        else -> {
                            writer.println("ERROR: UNKNOWN_COMMAND")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

object P2PClient {
    suspend fun fetchIndex(ip: String, port: Int): List<ContentHeader>? = withContext(Dispatchers.IO) {
        try {
            Socket(ip, port).use { socket ->
                socket.soTimeout = 5000
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                writer.println("INDEX")
                val response = reader.readLine()
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
            Socket(ip, port).use { socket ->
                socket.soTimeout = 10000
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                writer.println("GET $hash")
                val response = reader.readLine()
                if (response != null && response.startsWith("{")) {
                    return@withContext Json.decodeFromString<ContentUnit>(response)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
