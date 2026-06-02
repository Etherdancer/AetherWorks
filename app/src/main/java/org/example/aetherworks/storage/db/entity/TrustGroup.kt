package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trust_groups")
data class TrustGroup(
    @PrimaryKey val groupId: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
