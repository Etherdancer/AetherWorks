package org.example.aetherworks.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.aetherworks.storage.db.dao.ShoppingDao
import org.example.aetherworks.storage.db.entity.ShoppingItem
import java.util.UUID
import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent

class ShoppingViewModel(private val shoppingDao: ShoppingDao, private val context: Context) : ViewModel() {

    val items: StateFlow<List<ShoppingItem>> = shoppingDao.getAllShoppingItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addItem(name: String, reminderTimeMs: Long? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val item = ShoppingItem(
                id = UUID.randomUUID().toString(),
                name = name,
                reminderTime = reminderTimeMs
            )
            withContext(Dispatchers.IO) {
                shoppingDao.insertItem(item)
            }
            if (reminderTimeMs != null) {
                scheduleAlarm(item)
            }
        }
    }

    private fun scheduleAlarm(item: ShoppingItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ShoppingAlarmReceiver::class.java).apply {
            putExtra(ShoppingAlarmReceiver.EXTRA_ITEM_NAME, item.name)
            putExtra(ShoppingAlarmReceiver.EXTRA_ITEM_ID, item.id.hashCode().toLong())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Note: For Android 12+ we must ensure we have SCHEDULE_EXACT_ALARM permission
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                item.reminderTime!!,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Permission not granted, fallback or ignore
        }
    }

    fun toggleItem(item: ShoppingItem, isChecked: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingDao.updateCheckedStatus(item.id, isChecked)
            }
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingDao.deleteItem(item)
            }
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingDao.clearCompletedItems()
            }
        }
    }
}
