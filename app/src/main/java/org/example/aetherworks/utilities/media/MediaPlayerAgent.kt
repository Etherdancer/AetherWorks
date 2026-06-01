package org.example.aetherworks.utilities.media

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

/**
 * Offline Media Player Agent
 * Integrates ExoPlayer to safely play encrypted media from the Private Vault
 * without exporting it to tracking-prone system gallery apps.
 */
class MediaPlayerAgent(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null

    fun initializePlayer(): ExoPlayer {
        Log.d(TAG, "Initializing Offline Media Player")
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> Log.d(TAG, "Player Ready.")
                        Player.STATE_ENDED -> Log.d(TAG, "Playback Ended.")
                        Player.STATE_BUFFERING -> Log.d(TAG, "Player Buffering.")
                        Player.STATE_IDLE -> Log.d(TAG, "Player Idle.")
                    }
                }
            })
        }
        return exoPlayer!!
    }

    /**
     * Plays media securely from a local URI (which should be provided by a local ContentProvider
     * that decrypts chunks on the fly from the SQLCipher database).
     */
    fun playSecureMedia(localUri: Uri) {
        val player = exoPlayer ?: initializePlayer()
        val mediaItem = MediaItem.fromUri(localUri)
        
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        Log.d(TAG, "Started secure playback for URI: $localUri")
    }

    fun releasePlayer() {
        Log.d(TAG, "Releasing Offline Media Player")
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        private const val TAG = "MediaPlayerAgent"
    }
}
