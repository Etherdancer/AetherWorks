package app.clearspace.network.persona

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import app.clearspace.network.crypto.KeyManager

enum class VisibilityLevel {
    PRIVATE, TRUSTED, PUBLIC
}

@Serializable
data class ProfileField(
    val value: String,
    val visibility: VisibilityLevel
)

@Serializable
data class Profile(
    val publicKeyBase64: String,
    val encryptionPublicKeyBase64: String? = null,
    val alias: String,
    val avatarPath: String? = null,
    val avatarId: Int = 0,
    
    val dynamicStatus: ProfileField? = null,
    val pinnedContentHash: String? = null,
    
    val about: ProfileField? = null,
    val guidingPrinciple: ProfileField? = null,
    
    val favoriteMusic: ProfileField? = null,
    val favoriteMovies: ProfileField? = null,
    val favoriteTvShows: ProfileField? = null,
    val favoriteBooks: ProfileField? = null,
    val favoriteQuotes: ProfileField? = null,
    
    val interests: ProfileField? = null,
    
    val occupation: ProfileField? = null,
    val relationshipStatus: ProfileField? = null,
    val lookingFor: ProfileField? = null,
    val bodyType: ProfileField? = null,
    
    val westernZodiac: ProfileField? = null,
    val chineseZodiac: ProfileField? = null,
    val celticHoroscope: ProfileField? = null,
    val mayanKin: ProfileField? = null,
    val vedicRasi: ProfileField? = null
)

class PersonaAgent(context: Context, private val keyManager: KeyManager) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("aether_persona_prefs", Context.MODE_PRIVATE)
    
    private val ed25519Keys: Pair<ByteArray, ByteArray> by lazy {
        keyManager.getOrGenerateIdentity()
    }

    private val x25519Keys: Pair<ByteArray, ByteArray> by lazy {
        keyManager.getOrGenerateEncryptionIdentity()
    }

    val publicKeyBase64: String
        get() = Base64.encodeToString(ed25519Keys.second, Base64.NO_WRAP)

    val encryptionPublicKeyBase64: String
        get() = Base64.encodeToString(x25519Keys.second, Base64.NO_WRAP)

    fun getProfile(): Profile? {
        // FIX M1: Read encrypted profile JSON first; fall back to plaintext for migration
        val encryptedStr = prefs.getString("profile_json_enc", null)
        val jsonString: String? = if (encryptedStr != null) {
            try {
                val bytes = Base64.decode(encryptedStr, Base64.DEFAULT)
                String(keyManager.decryptData(bytes), Charsets.UTF_8)
            } catch (e: Exception) {
                null // Decryption failure — treat as no profile
            }
        } else {
            // Migrate legacy plaintext profile_json to encrypted storage
            val legacy = prefs.getString("profile_json", null)
            if (legacy != null) {
                // Re-save as encrypted; the next getProfile() call will use the encrypted path
                try {
                    val enc = keyManager.encryptData(legacy.toByteArray(Charsets.UTF_8))
                    prefs.edit()
                        .putString("profile_json_enc", Base64.encodeToString(enc, Base64.DEFAULT))
                        .remove("profile_json")
                        .apply()
                } catch (e: Exception) { /* Continue with plaintext if migration fails */ }
                legacy
            } else null
        }

        if (jsonString != null) {
            return try {
                Json.decodeFromString<Profile>(jsonString)
            } catch (e: Exception) {
                null
            }
        }
        
        // Fallback for previous version (alias-only storage)
        val alias = prefs.getString("alias", null) ?: return null
        val bio = prefs.getString("bio", "") ?: ""
        val avatarId = prefs.getInt("avatarId", 0)
        
        val migratedProfile = Profile(
            publicKeyBase64 = publicKeyBase64,
            encryptionPublicKeyBase64 = encryptionPublicKeyBase64,
            alias = alias,
            avatarId = avatarId,
            about = if (bio.isNotEmpty()) ProfileField(bio, VisibilityLevel.PUBLIC) else null
        )
        saveProfile(migratedProfile)
        return migratedProfile
    }

    fun saveProfile(profile: Profile) {
        require(profile.alias.isNotBlank()) { "Alias cannot be empty" }
        
        val jsonString = Json.encodeToString(profile.copy(
            publicKeyBase64 = publicKeyBase64,
            encryptionPublicKeyBase64 = encryptionPublicKeyBase64
        ))
        // FIX M1: Encrypt profile JSON before storing. The alias is kept as a
        // separate plaintext entry only because hasProfile() needs it without decryption.
        val encryptedBytes = keyManager.encryptData(jsonString.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("profile_json_enc", Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
            .remove("profile_json") // Remove any legacy plaintext entry
            .putString("alias", profile.alias)
            .apply()
    }

    fun hasProfile(): Boolean {
        return prefs.contains("profile_json_enc") || prefs.contains("profile_json") || prefs.contains("alias")
    }

    fun burnPersona() {
        prefs.edit().clear().apply()
        keyManager.regenerateIdentities()
    }

    var showProfileToNearbyUsers: Boolean
        get() = prefs.getBoolean("showProfileToNearbyUsers", false)
        set(value) = prefs.edit().putBoolean("showProfileToNearbyUsers", value).apply()
}
