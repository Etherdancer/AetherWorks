package org.example.aetherworks.utilities.media

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.PlaybackParameters

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.media.audiofx.Equalizer
import kotlinx.coroutines.*

enum class PlaybackState { IDLE, PLAYING, PAUSED, ERROR }

/**
 * Offline Media Player Agent
 * Integrates ExoPlayer to safely play encrypted media from the Private Vault
 * without exporting it to tracking-prone system gallery apps.
 */
class MediaPlayerAgent(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var sleepTimerJob: Job? = null
    private val agentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val currentPosition: Long
        get() = exoPlayer?.currentPosition ?: 0L

    fun initializePlayer(): ExoPlayer {
        Log.d(TAG, "Initializing Offline Media Player")
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            Log.d(TAG, "Player Ready.")
                            if (exoPlayer?.isPlaying == true) _playbackState.value = PlaybackState.PLAYING
                            else _playbackState.value = PlaybackState.PAUSED
                        }
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Playback Ended.")
                            _playbackState.value = PlaybackState.IDLE
                        }
                        Player.STATE_BUFFERING -> Log.d(TAG, "Player Buffering.")
                        Player.STATE_IDLE -> {
                            Log.d(TAG, "Player Idle.")
                            _playbackState.value = PlaybackState.IDLE
                        }
                    }
                }
                
                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                    _playbackState.value = PlaybackState.ERROR
                }
            })
        }
        setupEqualizer(exoPlayer!!.audioSessionId)
        return exoPlayer!!
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            Log.d(TAG, "Equalizer initialized with ${equalizer?.numberOfBands} bands")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Equalizer", e)
        }
    }

    fun getEqualizer(): Equalizer? = equalizer

    /**
     * Plays media securely from a local URI (which should be provided by a local ContentProvider
     * that decrypts chunks on the fly from the SQLCipher database).
     */
    fun playSecureMedia(localUri: Uri, startPosition: Long = 0L, speed: Float = 1.0f) {
        val player = exoPlayer ?: initializePlayer()
        val mediaItem = MediaItem.fromUri(localUri)
        
        player.setMediaItem(mediaItem)
        if (startPosition > 0L) {
            player.seekTo(startPosition)
        }
        player.playbackParameters = PlaybackParameters(speed)
        player.prepare()
        player.play()
        _playbackState.value = PlaybackState.PLAYING
        Log.d(TAG, "Started secure playback for URI: $localUri at $startPosition ms with speed $speed")
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun pause() {
        exoPlayer?.pause()
        _playbackState.value = PlaybackState.PAUSED
    }

    fun resume() {
        exoPlayer?.play()
        _playbackState.value = PlaybackState.PLAYING
    }

    fun stop() {
        exoPlayer?.stop()
        _playbackState.value = PlaybackState.IDLE
        cancelSleepTimer()
    }

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        sleepTimerJob = agentScope.launch {
            Log.d(TAG, "Sleep timer set for $minutes minutes")
            delay(minutes * 60 * 1000L)
            Log.d(TAG, "Sleep timer triggered, stopping playback")
            pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    fun releasePlayer() {
        Log.d(TAG, "Releasing Offline Media Player")
        cancelSleepTimer()
        equalizer?.release()
        equalizer = null
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        private const val TAG = "MediaPlayerAgent"
    }
}
