package org.example.aetherworks.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.aetherworks.storage.db.entity.Message

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Query("SELECT * FROM messages WHERE (senderPublicKey = :peerKey OR receiverPublicKey = :peerKey) ORDER BY timestamp ASC")
    suspend fun getConversation(peerKey: String): List<Message>

    @Query("DELETE FROM messages WHERE ttl > 0 AND (timestamp + ttl) < :currentTime")
    suspend fun deleteExpired(currentTime: Long): Int
}
