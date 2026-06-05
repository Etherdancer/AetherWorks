package app.clearspace.network.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_blacklist")
data class BlacklistEntry(
    @PrimaryKey val hash: String,
    val timestamp: Long
)
