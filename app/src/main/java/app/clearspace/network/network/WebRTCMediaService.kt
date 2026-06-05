package app.clearspace.network.network

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Service responsible for managing WebRTC Data Channels.
 * Used for streaming large media files (images/videos) directly device-to-device 
 * for TRUSTED and GROUP visibility tiers.
 * 
 * Flow:
 * 1. Device A uploads a tiny WebRTC signaling offer to FirestoreDeadDropService.
 * 2. Device B downloads the offer, generates an answer, and uploads to Firestore.
 * 3. Devices establish direct connection via ICE servers (STUN/TURN).
 * 4. File is streamed over RTCDataChannel, bypassing Firebase bandwidth.
 */
class WebRTCMediaService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_SIGNALING) {
            val contentHash = intent.getStringExtra(EXTRA_CONTENT_HASH)
            Log.d("WebRTCMedia", "Starting WebRTC signaling for hash: $contentHash")
            // TODO: Initialize PeerConnectionFactory, create DataChannel, send offer via Firestore.
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_START_SIGNALING = "app.clearspace.network.START_WEBRTC_SIGNALING"
        const val EXTRA_CONTENT_HASH = "extra_content_hash"
    }
}
