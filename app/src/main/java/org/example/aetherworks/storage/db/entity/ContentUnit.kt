package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Visibility {
    PRIVATE, TRUSTED, PUBLIC
}

@Entity(tableName = "content_units")
data class ContentUnit(
    @PrimaryKey val contentHash: String,
    val title: String,
    val body: String,
    val categoryFlags: String,
    val emotionFlags: String,
    val visibility: Visibility,
    val authorAlias: String,
    val timestamp: Long,
    val importCount: Int,
    val powNonce: Long,
    val likeTokens: Set<String>,
    val dislikeTokens: Set<String>,
    val categoryTokens: Map<String, Set<String>> = emptyMap(),
    val emotionTokens: Map<String, Set<String>> = emptyMap(),
    val imagePath: String? = null,
    val videoPath: String? = null,
    val thumbnailPath: String? = null,
    val thumbnailBase64: String? = null
)
