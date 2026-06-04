package app.clearspace.network.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_authors")
data class BlockedAuthor(
    @PrimaryKey val authorId: String,
    val blockedAt: Long = System.currentTimeMillis()
)
