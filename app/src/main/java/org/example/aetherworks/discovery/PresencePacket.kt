package org.example.aetherworks.discovery

import kotlinx.serialization.Serializable

/**
 * Lightweight packet broadcast over mDNS, BLE, and Wi-Fi Direct.
 * Fits within strict size limits of TXT records and BLE advertisement payloads.
 */
@Serializable
data class PresencePacket(
    val peerId: String, // Ephemeral unique ID for this session (UUID substring)
    val hasProfile: Boolean,
    val categoryBitmask: Long, // 64-bit integer representing up to 64 selected categories
    val tcpPort: Int,
    val timestamp: Long = System.currentTimeMillis()
)
