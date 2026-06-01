package org.example.aetherworks.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.aetherworks.storage.db.dao.CalendarDao
import org.example.aetherworks.storage.db.entity.CalendarEvent
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class CalendarViewModel(private val calendarDao: CalendarDao) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Only get regular events (eventType == 0)
    val events: StateFlow<List<CalendarEvent>> = calendarDao.getAllEvents()
        .map { all -> all.filter { it.eventType == 0 }.sortedBy { it.startTs } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addEvent(title: String, description: String, startTs: Long, endTs: Long) {
        viewModelScope.launch {
            val newEvent = CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                startTs = startTs,
                endTs = endTs,
                eventType = 0 // 0 for calendar event
            )
            calendarDao.insertEvent(newEvent)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            calendarDao.deleteEventById(id)
        }
    }
}
