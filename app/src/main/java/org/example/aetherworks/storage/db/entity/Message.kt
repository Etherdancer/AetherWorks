package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderPublicKey: String,
    val receiverPublicKey: String,
    val encryptedPayload: String,
    val timestamp: Long,
    val delivered: Boolean,
    val ttl: Long
)
