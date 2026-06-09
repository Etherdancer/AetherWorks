package app.clearspace.network.security

import app.clearspace.network.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

class VersionCheckRepository {

    /**
     * Checks the latest release from GitHub.
     * Returns the download URL if an update is required, or null if the app is up to date or the check fails.
     */
    suspend fun checkForUpdate(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/Etherdancer/ClearSpace/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val tagName = json.getString("tag_name")
                val htmlUrl = json.getString("html_url")
                
                val latestVersion = parseVersion(tagName)
                val currentVersion = parseVersion(BuildConfig.VERSION_NAME)
                
                Log.d("VersionCheck", "Latest: $latestVersion, Current: $currentVersion")

                // Simple version comparison logic: check if latest > current
                if (isNewerVersion(currentVersion, latestVersion)) {
                    return@withContext htmlUrl
                }
            } else {
                Log.e("VersionCheck", "Failed to fetch version: HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("VersionCheck", "Error checking for updates", e)
        }
        return@withContext null
    }

    private fun parseVersion(versionStr: String): List<Int> {
        val sanitized = versionStr.replace(Regex("[^0-9.]"), "")
        return sanitized.split('.').mapNotNull { it.toIntOrNull() }
    }

    private fun isNewerVersion(current: List<Int>, latest: List<Int>): Boolean {
        val maxLength = maxOf(current.size, latest.size)
        for (i in 0 until maxLength) {
            val c = current.getOrNull(i) ?: 0
            val l = latest.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false // Identical versions
    }
}
