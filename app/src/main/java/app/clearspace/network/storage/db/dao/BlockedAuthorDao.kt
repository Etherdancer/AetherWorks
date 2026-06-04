package app.clearspace.network.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import app.clearspace.network.storage.db.entity.BlockedAuthor

@Dao
interface BlockedAuthorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(author: BlockedAuthor): Long

    @Query("DELETE FROM blocked_authors WHERE authorId = :authorId")
    suspend fun delete(authorId: String): Int

    @Query("SELECT * FROM blocked_authors")
    fun getAllBlocked(): Flow<List<BlockedAuthor>>
}
