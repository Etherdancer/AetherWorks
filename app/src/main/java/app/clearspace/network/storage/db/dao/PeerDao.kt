package app.clearspace.network.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.clearspace.network.storage.db.entity.KnownPeer
import app.clearspace.network.storage.db.entity.TrustLevel

@Dao
interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(peer: KnownPeer): Long

    @Query("SELECT * FROM known_peers")
    fun getAllPeers(): kotlinx.coroutines.flow.Flow<List<KnownPeer>>

    @Query("SELECT * FROM known_peers WHERE publicKeyBase64 = :publicKey LIMIT 1")
    suspend fun getByPublicKey(publicKey: String): KnownPeer?

    @Query("SELECT * FROM known_peers WHERE trustLevel = :trustLevel")
    suspend fun getByTrustLevel(trustLevel: TrustLevel): List<KnownPeer>

    @Query("DELETE FROM known_peers WHERE publicKeyBase64 = :publicKey")
    suspend fun delete(publicKey: String): Int
}
