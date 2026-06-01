package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_notes")
data class VaultNote(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val markdownContent: String,
    val tags: String = "", // Comma separated tags
    val folder: String = "Root",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
