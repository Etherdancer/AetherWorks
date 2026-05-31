package org.example.aetherworks.discovery

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.example.aetherworks.MainActivity
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.persona.PersonaAgent

class DiscoveryForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "aetherworks_sharing_channel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP_SHARING = "org.example.aetherworks.ACTION_STOP_SHARING"
    }

    private var discoveryManager: DiscoveryManager? = null
    private val ephemeralPeerId = java.util.UUID.randomUUID().toString().substring(0, 8)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Initialize discovery manager with NSD fallback
        val nsdDiscovery = NsdDiscovery(this)
        discoveryManager = DiscoveryManager(listOf(nsdDiscovery))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SHARING) {
            discoveryManager?.stop()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (ex2: Exception) {
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (ex3: Exception) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        val personaAgent = PersonaAgent(this, KeyManager(this))

        // Create presence packet. Port 0 means we're just broadcasting presence without a P2P server running yet.
        val packet = PresencePacket(
            peerId = ephemeralPeerId,
            hasProfile = personaAgent.hasProfile(),
            categoryBitmask = 0L,
            tcpPort = 0
        )
        discoveryManager?.start(packet)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AetherWorks Sharing"
            val descriptionText = "Persistent notification when sharing is active"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        val stopIntent = Intent(this, DiscoveryForegroundService::class.java).apply {
            action = ACTION_STOP_SHARING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AetherWorks Sharing is Active")
            .setContentText("Your device is discoverable and participating in the network.")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Sharing", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
