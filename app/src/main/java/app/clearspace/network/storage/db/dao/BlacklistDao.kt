package app.clearspace.network.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.clearspace.network.storage.db.entity.BlacklistEntry

@Dao
interface BlacklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BlacklistEntry>): List<Long>

    @Query("SELECT * FROM cached_blacklist")
    suspend fun getAll(): List<BlacklistEntry>

    @Query("DELETE FROM cached_blacklist")
    suspend fun clear(): Int
}
