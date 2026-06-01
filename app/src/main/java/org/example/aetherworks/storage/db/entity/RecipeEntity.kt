package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.example.aetherworks.storage.db.Converters

@Entity(tableName = "recipes")
@TypeConverters(Converters::class)
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val isFavorite: Boolean = false
)
