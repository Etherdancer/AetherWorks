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
        val dangerousProps = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0",
            "ro.build.selinux" to "0"
        )
        return try {
            dangerousProps.any { (prop, dangerousValue) ->
                val process = Runtime.getRuntime().exec(arrayOf("getprop", prop))
                val result = process.inputStream.bufferedReader().readLine()?.trim() ?: ""
                result == dangerousValue
            }
        } catch (e: Exception) {
            false // If we can't execute getprop, don't block the app
        }
    }
}
