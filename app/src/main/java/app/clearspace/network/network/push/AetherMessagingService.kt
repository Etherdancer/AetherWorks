package app.clearspace.network.network.push

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import app.clearspace.network.discovery.DiscoveryManager

class AetherMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("AetherMessagingService", "Received a wake-up ping from Firebase!")
        
        // This is where we receive a ping that a peer wants to connect.
        // Even if the app is closed, this will wake up the process.
        // We will temporarily start the DiscoveryManager to look for them,
        // sync the pending message, and then shut down.
        
        // In a full implementation, we'd check the payload to see if it's
        // a valid ping, but for the MVP hybrid stub, we just kickstart discovery.
        
        val startIntent = Intent(this, app.clearspace.network.discovery.DiscoveryForegroundService::class.java)
        startIntent.action = "WAKE_UP_PING"
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
        } catch (e: Exception) {
            Log.e("AetherMessagingService", "Could not start service from background: ${e.message}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("AetherMessagingService", "New FCM Token: $token")
        
        // Here we would encrypt this token with our Public Key and send it
        // to our minimal Firebase backend so peers know how to ping us.
    }
}
