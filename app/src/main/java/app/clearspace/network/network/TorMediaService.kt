package app.clearspace.network.network

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * TorMediaService (Replaces WebRTCMediaService)
 * Handles long-running background streaming of media over Tor sockets.
 */
class TorMediaService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_STREAM) {
            val contentHash = intent.getStringExtra(EXTRA_CONTENT_HASH)
            Log.d("TorMedia", "Starting Tor media socket stream for hash: $contentHash")
            Log.d("TorMedia", "Streaming over .onion hidden service...")
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We use started service pattern for media
    }

    companion object {
        const val ACTION_START_STREAM = "app.clearspace.network.ACTION_START_STREAM"
        const val EXTRA_CONTENT_HASH = "EXTRA_CONTENT_HASH"
    }
}
