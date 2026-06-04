package app.clearspace.network.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_logs")
data class SecurityLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val timestamp: Long,
    val detail: String
)
