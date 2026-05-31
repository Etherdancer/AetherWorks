package org.example.aetherworks.persona

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.example.aetherworks.crypto.KeyManager

data class Profile(
    val publicKeyBase64: String,
    val alias: String,
    val bio: String,
    val avatarId: Int // Built-in avatar reference
)

class PersonaAgent(context: Context, private val keyManager: KeyManager) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("aether_persona_prefs", Context.MODE_PRIVATE)
    
    // Cache the identity keypair
    private val ed25519Keys: Pair<ByteArray, ByteArray> by lazy {
        keyManager.getOrGenerateIdentity()
    }

    val publicKeyBase64: String
        get() = Base64.encodeToString(ed25519Keys.second, Base64.NO_WRAP)

    fun getProfile(): Profile? {
        val alias = prefs.getString("alias", null) ?: return null
        val bio = prefs.getString("bio", "") ?: ""
        val avatarId = prefs.getInt("avatarId", 0)
        
        return Profile(publicKeyBase64, alias, bio, avatarId)
    }

    fun saveProfile(alias: String, bio: String, avatarId: Int) {
        require(alias.isNotBlank()) { "Alias cannot be empty" }
        require(bio.length <= 200) { "Bio must be 200 characters or less" }

        prefs.edit()
            .putString("alias", alias)
            .putString("bio", bio)
            .putInt("avatarId", avatarId)
            .apply()
    }

    fun hasProfile(): Boolean {
        return prefs.contains("alias")
    }
}
