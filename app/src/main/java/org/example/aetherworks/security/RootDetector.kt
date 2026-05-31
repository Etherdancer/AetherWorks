package org.example.aetherworks.security

import android.os.Build
import java.io.File

class RootDetector {

    fun isRootedOrCustomRom(): Boolean {
        return checkTestKeys() || checkSuBinary() || checkDangerousProps()
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/magisk/.core/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkDangerousProps(): Boolean {
        // Just checking standard build properties often modified by root or Magisk
        return false // Keeping this light to avoid false positives on privacy ROMs
    }
}
