package app.clearspace.network.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.clearspace.network.storage.db.entity.SecurityLogEntry

@Dao
interface SecurityLogDao {
    @Insert
    suspend fun insert(entry: SecurityLogEntry): Long

    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<SecurityLogEntry>
}
