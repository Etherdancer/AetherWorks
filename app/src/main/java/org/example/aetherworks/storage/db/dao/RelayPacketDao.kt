package org.example.aetherworks.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.example.aetherworks.storage.db.entity.RelayPacket

@Dao
interface RelayPacketDao {
    @Query("SELECT * FROM relay_packets")
    fun getAllRelayPackets(): Flow<List<RelayPacket>>

    @Query("SELECT * FROM relay_packets WHERE ttlExpiration > :currentTime")
    suspend fun getValidRelayPackets(currentTime: Long): List<RelayPacket>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPacket(packet: RelayPacket): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPackets(packets: List<RelayPacket>): List<Long>

    @Query("DELETE FROM relay_packets WHERE ttlExpiration <= :currentTime")
    suspend fun deleteExpiredPackets(currentTime: Long): Int

    @Query("DELETE FROM relay_packets WHERE packetId IN (SELECT packetId FROM relay_packets ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestPackets(count: Int): Int

    @Query("SELECT SUM(LENGTH(encryptedPayload)) FROM relay_packets")
    suspend fun getTotalPayloadSize(): Long?
}
