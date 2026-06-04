package app.clearspace.network.storage.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import app.clearspace.network.storage.db.entity.TrustLevel
import app.clearspace.network.storage.db.entity.Visibility

class Converters {
    @TypeConverter
    fun fromVisibility(value: Visibility): String = value.name

    @TypeConverter
    fun toVisibility(value: String): Visibility = enumValueOf<Visibility>(value)

    @TypeConverter
    fun fromTrustLevel(value: TrustLevel): String = value.name

    @TypeConverter
    fun toTrustLevel(value: String): TrustLevel = enumValueOf<TrustLevel>(value)

    @TypeConverter
    fun fromStringSet(value: Set<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringSet(value: String): Set<String> = if (value.isBlank()) emptySet() else Json.decodeFromString(value)

    @TypeConverter
    fun fromStringMapSet(value: Map<String, Set<String>>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringMapSet(value: String): Map<String, Set<String>> = if (value.isBlank()) emptyMap() else Json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isBlank()) emptyList() else Json.decodeFromString(value)
}
