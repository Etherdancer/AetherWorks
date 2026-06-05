package app.clearspace.network.security

import android.os.Build
import java.io.File

class RootDetector {

    fun isRootedOrCustomRom(): Boolean {
        return checkTestKeys() || checkSuBinary() || checkDangerousProps() || checkXposed()
    }

    private fun checkXposed(): Boolean {
        try {
            val xposedClass = Class.forName("de.robv.android.xposed.XposedBridge")
            if (xposedClass != null) return true
        } catch (e: Exception) {
            // Class not found, good
        }
        try {
            val stackTrace = Throwable().stackTrace
            for (element in stackTrace) {
                if (element.className.contains("xposed") || 
                    element.className.contains("edxposed") || 
                    element.className.contains("lsposed")) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        val xposedPaths = arrayOf(
            "/system/framework/XposedBridge.jar",
            "/data/data/de.robv.android.xposed.installer",
            "/system/bin/app_process64_xposed",
            "/system/bin/app_process32_xposed"
        )
        return xposedPaths.any { File(it).exists() }
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
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
            
            dangerousProps.any { (prop, dangerousValue) ->
                val result = getMethod.invoke(null, prop, "") as String
                result == dangerousValue
            }
        } catch (e: Exception) {
            false
        }
    }
}
