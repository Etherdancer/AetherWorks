package org.example.aetherworks.storage.db

import androidx.room.TypeConverter
import org.example.aetherworks.storage.db.entity.TrustLevel
import org.example.aetherworks.storage.db.entity.Visibility

class Converters {
    @TypeConverter
    fun fromVisibility(value: Visibility): String = value.name

    @TypeConverter
    fun toVisibility(value: String): Visibility = enumValueOf<Visibility>(value)

    @TypeConverter
    fun fromTrustLevel(value: TrustLevel): String = value.name

    @TypeConverter
    fun toTrustLevel(value: String): TrustLevel = enumValueOf<TrustLevel>(value)
}
