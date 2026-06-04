package app.clearspace.network.storage.sync

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

interface SyncManager {
    fun startSyncService()
    fun stopSyncService()
    fun getSyncStatus(): Flow<SyncStatus>
    fun addTrustedDevice(deviceId: String)
    fun removeTrustedDevice(deviceId: String)
}

enum class SyncStatus {
    STOPPED, STARTING, SCANNING, SYNCING, IDLE, ERROR
}


