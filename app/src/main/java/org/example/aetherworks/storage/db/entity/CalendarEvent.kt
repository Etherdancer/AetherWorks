package org.example.aetherworks.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTs: Long, // Start time in milliseconds
    val endTs: Long,   // End time in milliseconds
    val isAllDay: Boolean = false,
    val color: Int = 0, // ARGB color
    val reminderMinutes: Int = -1, // -1 means no reminder
    val recurrenceRule: String = "", // RRULE format
    val eventType: Int = 0, // 0 for regular event, 1 for task/habit
    val isDone: Boolean = false
)
