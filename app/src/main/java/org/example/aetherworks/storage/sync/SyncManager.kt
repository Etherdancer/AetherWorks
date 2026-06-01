package org.example.aetherworks.storage.sync

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

/**
 * Placeholder implementation for Syncthing BEP integration.
 * Will require syncthing-android daemon to be bundled.
 */
class SyncthingManager(private val context: Context, private val dbFile: File) : SyncManager {
    private val _syncStatus = MutableStateFlow(SyncStatus.STOPPED)

    override fun startSyncService() {
        // TODO: Initialize Syncthing native binary with folder configured to watch dbFile.parentFile
        _syncStatus.value = SyncStatus.IDLE
    }

    override fun stopSyncService() {
        // TODO: Shutdown Syncthing native daemon
        _syncStatus.value = SyncStatus.STOPPED
    }

    override fun getSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()

    override fun addTrustedDevice(deviceId: String) {
        // TODO: Call Syncthing REST API to add device and share the Vault folder
    }

    override fun removeTrustedDevice(deviceId: String) {
        // TODO: Call Syncthing REST API to remove device
    }
}
