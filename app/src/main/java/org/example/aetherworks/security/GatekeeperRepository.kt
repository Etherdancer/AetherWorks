package org.example.aetherworks.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.crypto.PasswordHasher

class GatekeeperRepository(private val context: Context, private val keyManager: KeyManager) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gatekeeper_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(PREF_ONBOARDING_COMPLETE, false)
    }

    fun completeOnboarding(password: String): ByteArray {
        val (salt, hash) = PasswordHasher.hash(password.toCharArray(), clearPassword = false)
        val fullHash = salt + hash
        keyManager.storePasswordHash(fullHash)
        prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, true).apply()
        return keyManager.deriveSqlCipherKey(hash)
    }

    fun isLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(PREF_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getRemainingLockoutSeconds(): Long {
        val lockoutUntil = prefs.getLong(PREF_LOCKOUT_UNTIL, 0L)
        val remaining = lockoutUntil - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 else 0
    }

    fun authenticate(password: String): ByteArray? {
        if (isLockedOut()) return null

        val storedHash = keyManager.getStoredPasswordHash() ?: return null
        val computedHash = PasswordHasher.computeHashForDbKey(password, storedHash)

        if (computedHash != null) {
            resetFailedAttempts()
            _authState.value = AuthState.Authenticated
            return keyManager.deriveSqlCipherKey(computedHash)
        } else {
            recordFailedAttempt()
            return null
        }
    }

    private fun recordFailedAttempt() {
        val attempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit()
        editor.putInt(PREF_FAILED_ATTEMPTS, attempts)

        if (attempts >= 20) {
            // Self-destruct condition: Wipe keys
            keyManager.wipeAllKeys()
            // In a real app we'd also clear the DB files here, but since the keys are gone, the DB is cryptographically shredded.
            editor.putLong(PREF_LOCKOUT_UNTIL, Long.MAX_VALUE) // Permanent lockout
        } else if (attempts >= 5) {
            val lockoutMinutes = when {
                attempts >= 15 -> 15.0
                attempts >= 10 -> 5.0
                attempts >= 7 -> 1.0
                else -> 0.5 // 30 seconds
            }
            val lockoutUntil = System.currentTimeMillis() + (lockoutMinutes * 60.0 * 1000.0).toLong()
            editor.putLong(PREF_LOCKOUT_UNTIL, lockoutUntil)
        }
        editor.apply()
    }

    private fun resetFailedAttempts() {
        prefs.edit()
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .putLong(PREF_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    companion object {
        private const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val PREF_FAILED_ATTEMPTS = "failed_attempts"
        private const val PREF_LOCKOUT_UNTIL = "lockout_until"
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
}
