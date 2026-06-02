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
import kotlinx.coroutines.launch
import org.example.aetherworks.MainActivity
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.persona.PersonaAgent
import org.example.aetherworks.storage.db.AetherDatabase

class DiscoveryForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "aetherworks_sharing_channel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP_SHARING = "org.example.aetherworks.ACTION_STOP_SHARING"
    }

    private var discoveryManager: DiscoveryManager? = null
    private var p2pServer: P2PServer? = null
    private val ephemeralPeerId = java.util.UUID.randomUUID().toString().substring(0, 8)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Initialize discovery manager with NSD, BLE, and Wi-Fi Direct
        val nsdDiscovery = NsdDiscovery(this)
        val bleDiscovery = BleDiscovery(this)
        val wifiDirectDiscovery = WifiDirectDiscovery(this)
        discoveryManager = DiscoveryManager(listOf(nsdDiscovery, bleDiscovery, wifiDirectDiscovery))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SHARING) {
            discoveryManager?.stop()
            p2pServer?.stop()
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
        val db = try {
            AetherDatabase.getSharedDatabase()
        } catch (e: Exception) {
            // Service started but DB not initialized? Should not happen if UI enforces unlock first.
            stopSelf()
            return START_NOT_STICKY
        }

        p2pServer = P2PServer(this, db)
        val serverPort = p2pServer?.start() ?: 0

        // Create presence packet.
        val packet = PresencePacket(
            peerId = ephemeralPeerId,
            hasProfile = personaAgent.hasProfile(),
            categoryBitmask = 0L,
            tcpPort = serverPort
        )
        discoveryManager?.start(packet)

        // Background Relay Synchronization
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        coroutineScope.launch {
            discoveryManager?.discoveredPeers?.collect { peers ->
                peers.forEach { peer ->
                    val peerIp = peer.ip
                    if (peerIp != null && peer.tcpPort > 0) {
                        try {
                            val prefs = getSharedPreferences("aether_settings", Context.MODE_PRIVATE)
                            val quotaMb = prefs.getInt("relay_quota_mb", 500)
                            val maxBytes = quotaMb * 1024L * 1024L
                            
                            val myPubKey = personaAgent.publicKeyBase64
                            val digest = java.security.MessageDigest.getInstance("SHA-256")
                            val myHashedIdBytes = digest.digest(myPubKey.toByteArray())
                            val myHashedId = myHashedIdBytes.joinToString("") { "%02x".format(it) }
                            
                            val packetIds = P2PClient.fetchRelayIndex(peerIp, peer.tcpPort) ?: emptyList()
                            val relayDao = db.relayPacketDao()
                            val currentUsage = relayDao.getTotalPayloadSize() ?: 0L
                            
                            // Naive fetch
                            for (id in packetIds) {
                                // Check if we already have it
                                val exists = relayDao.getValidRelayPackets(System.currentTimeMillis()).any { it.packetId == id }
                                if (!exists) {
                                    val fetched = P2PClient.fetchRelayPacket(peerIp, peer.tcpPort, id)
                                    if (fetched != null) {
                                        val isForMe = (fetched.hashedRecipientId == myHashedId)
                                        if (isForMe || currentUsage < maxBytes) {
                                            relayDao.insertPacket(fetched)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RelaySync", "Failed to sync relay with peer", e)
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager?.stop()
        p2pServer?.stop()
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
