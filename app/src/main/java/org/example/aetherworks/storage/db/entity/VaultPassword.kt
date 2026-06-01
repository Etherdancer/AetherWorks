package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_passwords")
data class VaultPassword(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val username: String,
    val encryptedPasswordBlob: String, // Encrypted payload (for an extra layer or raw if relying entirely on SQLCipher)
    val url: String = "",
    val notes: String = "",
    val category: String = "General",
    val totpSecret: String = "", // For Aegis/2FAS OTP functionality
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
