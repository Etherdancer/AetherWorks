package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relay_packets")
data class RelayPacket(
    @PrimaryKey val packetId: String, // e.g., SHA-256 of the payload to prevent duplicates
    val hashedRecipientId: String, // Hash of the recipient's public key
    val senderAlias: String?, // Optional, mostly anonymous
    val encryptedPayload: ByteArray, // The actual encrypted message
    val timestamp: Long, // When the packet was created
    val ttlExpiration: Long // When the packet should be dropped
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RelayPacket

        if (packetId != other.packetId) return false

        return true
    }

    override fun hashCode(): Int {
        return packetId.hashCode()
    }
}
