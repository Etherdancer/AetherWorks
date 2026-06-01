package org.example.aetherworks.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.example.aetherworks.storage.db.dao.MediaDao
import org.example.aetherworks.storage.db.entity.MediaItem
import org.example.aetherworks.utilities.media.MediaPlayerAgent
import org.example.aetherworks.utilities.media.PlaybackState

class MediaViewModel(
    private val mediaDao: MediaDao,
    val mediaPlayerAgent: MediaPlayerAgent
) : ViewModel() {

    val history: StateFlow<List<MediaItem>> = mediaDao.getAllMedia()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = mediaPlayerAgent.playbackState

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val item = _currentMediaItem.value
                val state = playbackState.value
                if (item != null && state == PlaybackState.PLAYING) {
                    val pos = mediaPlayerAgent.currentPosition
                    if (pos > 0) {
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            mediaDao.updatePlaybackState(item.id, pos, item.playbackSpeed)
                        }
                    }
                }
            }
        }
    }

    fun playMedia(item: MediaItem) {
        viewModelScope.launch {
            _currentMediaItem.value = item
            mediaPlayerAgent.playSecureMedia(android.net.Uri.parse(item.filePath), item.lastPlaybackPosition, item.playbackSpeed)
        }
    }

    fun playMedia(uri: String, title: String) {
        viewModelScope.launch {
            // Save to history
            val newMedia = MediaItem(
                id = java.util.UUID.randomUUID().toString(),
                fileName = title,
                filePath = uri,
                mimeType = "audio/video",
                sizeBytes = 0
            )
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                mediaDao.insertMedia(newMedia)
            }
            playMedia(newMedia)
        }
    }

    fun pause() {
        mediaPlayerAgent.pause()
    }

    fun resume() {
        mediaPlayerAgent.resume()
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaPlayerAgent.setPlaybackSpeed(speed)
        _currentMediaItem.value?.let { item ->
            val updatedItem = item.copy(playbackSpeed = speed)
            _currentMediaItem.value = updatedItem
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                mediaDao.updatePlaybackState(updatedItem.id, mediaPlayerAgent.currentPosition, speed)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayerAgent.seekTo(positionMs)
    }

    fun setSleepTimer(minutes: Int) {
        mediaPlayerAgent.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        mediaPlayerAgent.cancelSleepTimer()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerAgent.stop()
    }
}
