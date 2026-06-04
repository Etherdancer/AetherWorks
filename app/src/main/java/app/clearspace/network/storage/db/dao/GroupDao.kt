package app.clearspace.network.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import app.clearspace.network.storage.db.entity.GroupMember
import app.clearspace.network.storage.db.entity.KnownPeer
import app.clearspace.network.storage.db.entity.TrustGroup

data class GroupWithMembers(
    val group: TrustGroup,
    val members: List<KnownPeer>
)

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: TrustGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: GroupMember): Long

    @Query("DELETE FROM trust_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String): Int

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND publicKeyBase64 = :publicKey")
    suspend fun removeMember(groupId: String, publicKey: String): Int

    @Query("SELECT * FROM trust_groups")
    fun getAllGroups(): Flow<List<TrustGroup>>

    @Query("SELECT * FROM trust_groups WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: String): TrustGroup?

    @Query("""
        SELECT kp.* FROM known_peers kp
        INNER JOIN group_members gm ON kp.publicKeyBase64 = gm.publicKeyBase64
        WHERE gm.groupId = :groupId
    """)
    fun getGroupMembers(groupId: String): Flow<List<KnownPeer>>

    @Query("""
        SELECT kp.* FROM known_peers kp
        INNER JOIN group_members gm ON kp.publicKeyBase64 = gm.publicKeyBase64
        WHERE gm.groupId = :groupId
    """)
    suspend fun getGroupMembersSync(groupId: String): List<KnownPeer>
}
