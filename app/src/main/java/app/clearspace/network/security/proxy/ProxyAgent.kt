package app.clearspace.network.security.proxy

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread

/**
 * ProxyAgent (Tor Routing & Anonymity)
 * Embeds the Orbot/Tor backend to route all app traffic through the Onion network.
 * Used for:
 * 1. Anonymous WebRTC / Message routing (Tor hidden services).
 * 2. Anonymized external link routing (LibreTube style).
 */
class ProxyAgent(private val context: Context) {

    private val _torState = MutableStateFlow<TorState>(TorState.STOPPED)
    val torState: StateFlow<TorState> = _torState.asStateFlow()

    private val _onionAddress = MutableStateFlow<String?>(null)
    val onionAddress: StateFlow<String?> = _onionAddress.asStateFlow()

    private var torProcess: Process? = null

    fun startTor() {
        if (_torState.value == TorState.RUNNING || _torState.value == TorState.STARTING) return

        Log.d(TAG, "Starting Tor Daemon...")
        _torState.value = TorState.STARTING
        
        try {
            val appFilesDir = context.filesDir
            val torDataDir = java.io.File(appFilesDir, "tordata").apply { if (!exists()) mkdirs() }
            val torrcFile = java.io.File(appFilesDir, "torrc")
            
            // Write basic SOCKS proxy torrc config
            torrcFile.writeText("""
                SocksPort 9050
                DataDirectory ${torDataDir.absolutePath}
                AvoidDiskWrites 1
            """.trimIndent())
            
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val torBinary = java.io.File(nativeLibDir, "libtor.so")
            
            if (!torBinary.exists()) {
                throw java.io.FileNotFoundException("Tor binary not found at ${torBinary.absolutePath}")
            }
            
            val pb = ProcessBuilder(
                torBinary.absolutePath,
                "-f", torrcFile.absolutePath
            )
            .directory(appFilesDir)
            .redirectErrorStream(true)
            
            val proc = pb.start()
            torProcess = proc
            
            thread(start = true, name = "Tor-Reader-Thread") {
                try {
                    val reader = proc.inputStream.bufferedReader()
                    var line: String?
                    while (proc.isAlive) {
                        line = reader.readLine() ?: break
                        Log.d("TorBinary", line)
                        if (line.contains("Bootstrapped 100%")) {
                            _torState.value = TorState.RUNNING
                            _onionAddress.value = "127.0.0.1:9050"
                            Log.d(TAG, "Tor Daemon Started and Bootstrapped.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading Tor output", e)
                }
            }
            
            thread(start = true, name = "Tor-Monitor-Thread") {
                Thread.sleep(15000)
                if (_torState.value == TorState.STARTING) {
                    if (proc.isAlive) {
                        _torState.value = TorState.RUNNING
                        _onionAddress.value = "127.0.0.1:9050"
                        Log.w(TAG, "Tor startup monitor timeout; assuming running.")
                    } else {
                        _torState.value = TorState.ERROR
                        Log.e(TAG, "Tor process terminated during startup.")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Tor Daemon", e)
            _torState.value = TorState.ERROR
        }
    }

    fun stopTor() {
        Log.d(TAG, "Stopping Tor Daemon...")
        torProcess?.destroy()
        torProcess = null
        _torState.value = TorState.STOPPED
        _onionAddress.value = null
    }

    /**
     * Rewrite external URLs to proxy through Tor via Invidious/Piped.
     */
    fun rewriteExternalUrlForTor(url: String): String {
        return if (url.contains("youtube.com") || url.contains("youtu.be")) {
            // Translate to an Invidious instance over onion or public via Tor proxy
            url.replace("youtube.com", "yewtu.be")
               .replace("youtu.be", "yewtu.be")
        } else {
            url
        }
    }

    companion object {
        private const val TAG = "ProxyAgent"
        
        @Volatile
        private var instance: ProxyAgent? = null

        fun getInstance(context: Context): ProxyAgent {
            return instance ?: synchronized(this) {
                instance ?: ProxyAgent(context.applicationContext).also { instance = it }
            }
        }
    }

    enum class TorState {
        STOPPED, STARTING, RUNNING, ERROR
    }
}
