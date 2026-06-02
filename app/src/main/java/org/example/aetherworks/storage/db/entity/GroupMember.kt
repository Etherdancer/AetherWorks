package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "publicKeyBase64"],
    foreignKeys = [
        ForeignKey(
            entity = TrustGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KnownPeer::class,
            parentColumns = ["publicKeyBase64"],
            childColumns = ["publicKeyBase64"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["publicKeyBase64"])
    ]
)
data class GroupMember(
    val groupId: String,
    val publicKeyBase64: String
)
