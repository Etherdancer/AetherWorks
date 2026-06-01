package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_media")
data class MediaItem(
    @PrimaryKey
    val id: String, // UUID
    val fileName: String,
    val filePath: String, // Internal encrypted storage path
    val mimeType: String, // audio/mp3, video/mp4, image/jpeg
    val sizeBytes: Long,
    val durationMs: Long = 0, // For audio/video
    val folder: String = "Root",
    val addedAt: Long = System.currentTimeMillis()
)
