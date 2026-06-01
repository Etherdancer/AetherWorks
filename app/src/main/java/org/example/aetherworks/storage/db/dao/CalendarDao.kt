package org.example.aetherworks.storage.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.example.aetherworks.storage.db.entity.CalendarEvent

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events WHERE startTs >= :fromTs AND startTs <= :toTs ORDER BY startTs ASC")
    fun getEventsInRange(fromTs: Long, toTs: Long): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events ORDER BY startTs ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: CalendarEvent)

    @Delete
    fun deleteEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    fun deleteEventById(id: String)

    @Update
    fun updateEvent(event: CalendarEvent)

    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    fun getEventById(id: String): CalendarEvent?
}
