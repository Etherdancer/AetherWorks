package org.example.aetherworks.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.telephony.TelephonyManager
import java.io.File

class EmulatorDetector(private val context: Context) {

    fun isEmulator(): Boolean {
        return checkBuildProperties() || checkQemuFiles() || checkSensors() || checkTelephony()
    }

    private fun checkBuildProperties(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        val knownEmulatorStrings = listOf("generic", "sdk", "goldfish", "ranchu", "vbox", "nox", "bluestacks", "genymotion")

        for (str in knownEmulatorStrings) {
            if (fingerprint.contains(str) || model.contains(str) || manufacturer.contains(str) || product.contains(str) || hardware.contains(str)) {
                return true
            }
        }
        return false
    }

    private fun checkQemuFiles(): Boolean {
        val qemuFiles = listOf(
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props"
        )
        return qemuFiles.any { File(it).exists() }
    }

    private fun checkSensors(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorList = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: emptyList()
        // Most emulators have 0 or 1 sensor. Physical devices almost always have 5+.
        return sensorList.size <= 2
    }

    private fun checkTelephony(): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val networkOperatorName = telephonyManager?.networkOperatorName?.lowercase() ?: ""
        return networkOperatorName == "android" || networkOperatorName.contains("emulator")
    }
}
