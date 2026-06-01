package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey
    val id: String, // UUID
    val name: String,
    val isChecked: Boolean = false,
    val quantity: Int = 1,
    val reminderTime: Long? = null, // Epoch timestamp in milliseconds
    val createdAt: Long = System.currentTimeMillis()
)
