package org.example.aetherworks.storage

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.aetherworks.IAetherIpc
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.persona.PersonaAgent
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.entity.Visibility

class AetherDatabaseService : Service() {

    private val binder = object : IAetherIpc.Stub() {
        override fun getIndex(): String {
            return try {
                val db = AetherDatabase.getSharedDatabase()
                val publicContent = db.contentDao().getByVisibilitySync(Visibility.PUBLIC)
                val headers = publicContent.map { unit ->
                    org.example.aetherworks.discovery.ContentHeader(
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
                Json.encodeToString(headers)
            } catch (e: Exception) {
                "[]"
            }
        }

        override fun getContent(hash: String): String {
            return try {
                val db = AetherDatabase.getSharedDatabase()
                val unit = db.contentDao().getByHashSync(hash)
                if (unit != null && unit.visibility == Visibility.PUBLIC) {
                    Json.encodeToString(unit)
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }

        override fun getProfile(): String {
            return try {
                val personaAgent = PersonaAgent(this@AetherDatabaseService, KeyManager(this@AetherDatabaseService))
                if (personaAgent.showProfileToNearbyUsers) {
                    val profile = personaAgent.getProfile()
                    if (profile != null) {
                        Json.encodeToString(profile)
                    } else ""
                } else ""
            } catch (e: Exception) {
                ""
            }
        }

        override fun getRelayIndex(currentTime: Long): String {
            return try {
                val db = AetherDatabase.getSharedDatabase()
                val packets = kotlinx.coroutines.runBlocking {
                    db.relayPacketDao().getValidRelayPackets(currentTime)
                }
                Json.encodeToString(packets.map { it.packetId })
            } catch (e: Exception) {
                "[]"
            }
        }

        override fun getRelayPacket(currentTime: Long, packetId: String): String {
            return try {
                val db = AetherDatabase.getSharedDatabase()
                val packet = kotlinx.coroutines.runBlocking {
                    db.relayPacketDao().getValidRelayPackets(currentTime).find { it.packetId == packetId }
                }
                if (packet != null) Json.encodeToString(packet) else ""
            } catch (e: Exception) {
                ""
            }
        }

        override fun getRelayUsage(): Long {
            return try {
                val db = AetherDatabase.getSharedDatabase()
                kotlinx.coroutines.runBlocking {
                    db.relayPacketDao().getTotalPayloadSize() ?: 0L
                }
            } catch (e: Exception) {
                0L
            }
        }

        override fun getMyHashedId(): String {
            return try {
                val personaAgent = PersonaAgent(this@AetherDatabaseService, KeyManager(this@AetherDatabaseService))
                if (!personaAgent.hasProfile()) return ""
                val myPubKey = personaAgent.publicKeyBase64
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val myHashedIdBytes = digest.digest(myPubKey.toByteArray())
                myHashedIdBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                ""
            }
        }

        override fun hasRelayPacket(currentTime: Long, packetId: String): Boolean {
            return try {
                val db = AetherDatabase.getSharedDatabase()
                kotlinx.coroutines.runBlocking {
                    db.relayPacketDao().getValidRelayPackets(currentTime).any { it.packetId == packetId }
                }
            } catch (e: Exception) {
                false
            }
        }

        override fun insertRelayPacket(packetJson: String) {
            try {
                val packet = Json.decodeFromString<org.example.aetherworks.storage.db.entity.RelayPacket>(packetJson)
                val db = AetherDatabase.getSharedDatabase()
                kotlinx.coroutines.runBlocking {
                    db.relayPacketDao().insertPacket(packet)
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        override fun enforceStorageQuota() {
            kotlinx.coroutines.runBlocking {
                org.example.aetherworks.storage.StorageQuotaManager.enforcePublicQuota(this@AetherDatabaseService)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
