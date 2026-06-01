package org.example.aetherworks.storage.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.example.aetherworks.storage.db.entity.MediaItem

@Dao
interface MediaDao {
    @Query("SELECT * FROM vault_media ORDER BY addedAt DESC")
    fun getAllMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM vault_media WHERE mimeType LIKE 'audio/%' ORDER BY addedAt DESC")
    fun getAudioMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM vault_media WHERE mimeType LIKE 'video/%' ORDER BY addedAt DESC")
    fun getVideoMedia(): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMedia(item: MediaItem)

    @Delete
    fun deleteMedia(item: MediaItem)

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    fun getMediaById(id: String): MediaItem?

    @Query("UPDATE vault_media SET lastPlaybackPosition = :position, playbackSpeed = :speed WHERE id = :id")
    fun updatePlaybackState(id: String, position: Long, speed: Float)
}
