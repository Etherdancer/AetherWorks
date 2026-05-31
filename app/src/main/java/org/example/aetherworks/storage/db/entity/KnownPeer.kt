package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TrustLevel {
    ACQUAINTANCE,
    TRUSTED_REMOTE_UNVERIFIED,
    TRUSTED_REMOTE_VERIFIED,
    TRUSTED_IN_PERSON
}

@Entity(tableName = "known_peers")
data class KnownPeer(
    @PrimaryKey val publicKeyBase64: String,
    val alias: String,
    val avatarIndex: Int,
    val trustLevel: TrustLevel,
    val onionAddress: String?
)
