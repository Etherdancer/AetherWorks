package app.clearspace.network.tor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.tor.runtime.Action.Companion.startDaemonAsync
import io.matthewnelson.kmp.tor.runtime.Action.Companion.stopDaemonAsync
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import io.matthewnelson.kmp.tor.runtime.core.net.Port.Companion.toPort
import kotlinx.coroutines.delay

/**
 * TorManager initializes the embedded Tor daemon via kmp-tor.
 * It configures a v3 Hidden Service mapping an .onion address to a local port.
 */
class TorManager(private val context: Context) {
    private val _onionAddress = MutableStateFlow<String?>(null)
    val onionAddress: StateFlow<String?> = _onionAddress.asStateFlow()

    private val _isTorReady = MutableStateFlow(false)
    val isTorReady: StateFlow<Boolean> = _isTorReady.asStateFlow()
    
    private var runtime: TorRuntime? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startTorDaemon(localHiddenServicePort: Int) {
        Log.d(TAG, "Starting Tor Daemon...")
        Log.d(TAG, "Configuring Tor Hidden Service to forward to local port $localHiddenServicePort")
        
        scope.launch {
            try {
                val environment = TorRuntime.Environment.Builder(
                    workDirectory = File(context.getDir("tor", Context.MODE_PRIVATE).absolutePath),
                    cacheDirectory = File(context.getDir("tor_cache", Context.MODE_PRIVATE).absolutePath),
                    loader = { dir -> ResourceLoaderTorExec.getOrCreate(dir) }
                )

                val torRuntime = TorRuntime.Builder(environment) {
                    config { env ->
                        TorOption.HiddenServiceDir.tryConfigure {
                            directory(File(context.getDir("tor_hs", Context.MODE_PRIVATE).absolutePath))
                            version(3)
                            port(virtual = 80.toPort()) {
                                target(port = localHiddenServicePort.toPort())
                            }
                        }
                        TorOption.SocksPort.tryConfigure {
                            port(9050.toPort())
                        }
                    }
                }
                
                runtime = torRuntime
                torRuntime.startDaemonAsync()
                _isTorReady.value = true

                scope.launch {
                    val hostnameFile = java.io.File(context.getDir("tor_hs", Context.MODE_PRIVATE), "hostname")
                    var retries = 60
                    while (retries > 0 && _isTorReady.value) {
                        if (hostnameFile.exists()) {
                            val onion = hostnameFile.readText().trim()
                            _onionAddress.value = onion
                            Log.d(TAG, "Hidden Service is ready at: $onion")
                            break
                        }
                        delay(1000)
                        retries--
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Tor Service.", e)
            }
        }
    }

    fun stopTorDaemon() {
        Log.d(TAG, "Stopping Tor Daemon and destroying Hidden Service...")
        scope.launch {
            try {
                runtime?.stopDaemonAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop Tor Service.", e)
            }
        }
        _isTorReady.value = false
        _onionAddress.value = null
    }

    companion object {
        private const val TAG = "TorManager"
        @Volatile
        private var instance: TorManager? = null

        fun getInstance(context: Context): TorManager {
            return instance ?: synchronized(this) {
                instance ?: TorManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
