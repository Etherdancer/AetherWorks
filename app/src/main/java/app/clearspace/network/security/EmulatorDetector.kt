package app.clearspace.network.security

import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.telephony.TelephonyManager
import java.io.File

class EmulatorDetector(private val context: Context) {

    fun isEmulator(): Boolean {
        if (checkBuildProperties()) return true
        if (checkPipes()) return true
        if (checkSensors(context)) return true
        if (checkTelephony(context)) return true
        return false
    }

    private fun checkBuildProperties(): Boolean {
        val buildDetails = (Build.FINGERPRINT + Build.DEVICE + Build.MODEL + Build.BRAND + Build.PRODUCT + Build.MANUFACTURER + Build.HARDWARE).lowercase()
        return buildDetails.contains("generic") ||
               buildDetails.contains("emulator") ||
               buildDetails.contains("sdk") ||
               buildDetails.contains("vbox") ||
               buildDetails.contains("genymotion") ||
               buildDetails.contains("bluestacks") ||
               buildDetails.contains("nox") ||
               buildDetails.contains("goldfish") ||
               buildDetails.contains("ranchu") ||
               Build.BOARD.lowercase().contains("nox") ||
               Build.BOOTLOADER.lowercase().contains("nox") ||
               Build.HOST.lowercase().startsWith("build") ||
               Build.HOST.lowercase().contains("docker") ||
               (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    }

    private fun checkPipes(): Boolean {
        val knownPipes = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace"
        )
        for (pipe in knownPipes) {
            if (File(pipe).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkSensors(context: Context): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return true
        val sensorList = sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL)
        // Physical devices usually have 15+ sensors. Emulators have very few (0-3).
        return sensorList.size < 5
    }

    private fun checkTelephony(context: Context): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return true
        val networkOperatorName = telephonyManager.networkOperatorName ?: ""
        return networkOperatorName.lowercase() == "android" || networkOperatorName.lowercase() == "emulator"
    }
}
