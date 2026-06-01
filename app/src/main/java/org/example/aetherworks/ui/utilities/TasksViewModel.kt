package org.example.aetherworks.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.aetherworks.storage.db.dao.CalendarDao
import org.example.aetherworks.storage.db.entity.CalendarEvent
import java.util.UUID

class TasksViewModel(private val calendarDao: CalendarDao) : ViewModel() {
    // Only get task/habit events (eventType == 1)
    val tasks: StateFlow<List<CalendarEvent>> = calendarDao.getAllEvents()
        .map { all -> all.filter { it.eventType == 1 }.sortedBy { it.startTs } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, description: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newTask = CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                startTs = now,
                endTs = now,
                eventType = 1, // 1 for task/habit
                isDone = false
            )
            calendarDao.insertEvent(newTask)
        }
    }

    fun toggleTaskDone(task: CalendarEvent) {
        viewModelScope.launch {
            calendarDao.updateEvent(task.copy(isDone = !task.isDone))
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            calendarDao.deleteEventById(id)
        }
    }
}
