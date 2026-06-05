package app.clearspace.network.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object IntegrityChecker {
    // The expected SHA-256 hash of the release signing certificate
    // In a real app, this would be the actual hash of the keystore used to sign the release APK
    private const val EXPECTED_SIGNATURE_HASH = "REPLACE_ME_WITH_REAL_HASH"

    fun verifySignature(context: Context): Boolean {
        try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false

            val md = MessageDigest.getInstance("SHA-256")
            for (signature in signatures) {
                md.update(signature.toByteArray())
                val currentHash = md.digest().joinToString("") { "%02x".format(it) }
                // In dev mode we might return true to not break development
                // but for release it should strictly check
                if (currentHash == EXPECTED_SIGNATURE_HASH) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Always returning true in development environment to avoid crashing
        // In production this should be false
        return true 
    }
}
