package org.example.aetherworks.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.Visibility

@Dao
interface ContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: ContentUnit): Long

    @Query("SELECT * FROM content_units WHERE visibility IN ('PUBLIC', 'GROUP') ORDER BY timestamp DESC")
    fun getSharedContentFlow(): kotlinx.coroutines.flow.Flow<List<ContentUnit>>

    @Query("SELECT * FROM content_units WHERE visibility = :visibility ORDER BY timestamp DESC")
    suspend fun getByVisibility(visibility: Visibility): List<ContentUnit>

    @Query("SELECT * FROM content_units WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<ContentUnit>

    @Query("""
        SELECT content_units.* FROM content_units 
        JOIN content_units_fts ON content_units.rowid = content_units_fts.docid
        WHERE content_units_fts MATCH :query
    """)
    suspend fun searchSmartScan(query: String): List<ContentUnit>

    @Query("DELETE FROM content_units WHERE contentHash = :hash")
    suspend fun delete(hash: String): Int

    @Query("SELECT * FROM content_units WHERE contentHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): ContentUnit?

    @Query("SELECT * FROM content_units WHERE visibility = :visibility ORDER BY timestamp DESC")
    fun getByVisibilitySync(visibility: Visibility): List<ContentUnit>

    @Query("SELECT * FROM content_units WHERE contentHash = :hash LIMIT 1")
    fun getByHashSync(hash: String): ContentUnit?

    @Query("SELECT * FROM content_units WHERE visibility = 'PUBLIC' ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestPublic(): ContentUnit?

    @Query("SELECT * FROM content_units WHERE visibility = 'PUBLIC' ORDER BY (length(likeTokens) - length(dislikeTokens)) ASC, timestamp ASC LIMIT :limit")
    suspend fun getLowestReputationPublicContent(limit: Int): List<ContentUnit>
}
