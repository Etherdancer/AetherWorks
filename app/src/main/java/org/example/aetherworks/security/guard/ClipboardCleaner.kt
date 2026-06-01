package org.example.aetherworks.security.guard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Memory Guardian (Clipboard Cleaner)
 * Securely clears the clipboard after a set delay to prevent 
 * other apps from silently reading copied passwords or sensitive data.
 */
class ClipboardCleaner(private val context: Context) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var cleanJob: Job? = null

    /**
     * Copies sensitive text to the clipboard and schedules it to be cleared.
     * @param text The sensitive text to copy.
     * @param label A user-visible label for the clip (e.g., "Password").
     * @param clearAfterMs Time in milliseconds before the clipboard is automatically cleared.
     */
    fun copySensitiveText(text: String, label: String = "Sensitive Data", clearAfterMs: Long = 45000L) {
        val clipData = ClipData.newPlainText(label, text)
        
        // Android 13+ support to prevent the clipboard overlay from showing sensitive data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clipData.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        
        clipboardManager.setPrimaryClip(clipData)
        Log.d(TAG, "Sensitive data copied to clipboard. Will clear in ${clearAfterMs / 1000}s.")

        scheduleClear(clearAfterMs)
    }

    private fun scheduleClear(delayMs: Long) {
        cleanJob?.cancel()
        cleanJob = CoroutineScope(Dispatchers.Main).launch {
            delay(delayMs)
            clearClipboard()
        }
    }

    /**
     * Immediately clears the primary clip.
     */
    fun clearClipboard() {
        if (clipboardManager.hasPrimaryClip()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboardManager.clearPrimaryClip()
            } else {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            Log.d(TAG, "Clipboard cleared by Memory Guardian.")
        }
    }

    companion object {
        private const val TAG = "ClipboardCleaner"
        
        @Volatile
        private var instance: ClipboardCleaner? = null

        fun getInstance(context: Context): ClipboardCleaner {
            return instance ?: synchronized(this) {
                instance ?: ClipboardCleaner(context.applicationContext).also { instance = it }
            }
        }
    }
}
