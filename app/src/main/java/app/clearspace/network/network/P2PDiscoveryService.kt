package app.clearspace.network.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * P2PDiscoveryService
 * Manages local network discovery using Android NSD (mDNS).
 * Broadcasts this device's presence and listens for other ClearSpace nodes.
 */
class P2PDiscoveryService(private val context: Context) {

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPE = "_ClearSpace._tcp."
    private var serviceName = "AetherNode_${(1000..9999).random()}"

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /**
     * Registers this device on the local network.
     * @param port The port the TLS server socket is listening on.
     */
    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@P2PDiscoveryService.serviceName
            serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                serviceName = NsdServiceInfo.serviceName
                Log.d("P2PDiscovery", "Service registered: $serviceName on port $port")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("P2PDiscovery", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d("P2PDiscovery", "Service unregistered: ${arg0.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("P2PDiscovery", "Unregistration failed: $errorCode")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    /**
     * Starts discovering other ClearSpace nodes on the network.
     */
    fun discoverServices(onServiceFound: (NsdServiceInfo) -> Unit) {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("P2PDiscovery", "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d("P2PDiscovery", "Service discovery success: $service")
                if (service.serviceType == SERVICE_TYPE) {
                    if (service.serviceName != serviceName) {
                        // Found another node, attempt to resolve
                        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.e("P2PDiscovery", "Resolve failed: $errorCode")
                            }

                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                Log.d("P2PDiscovery", "Resolve Succeeded. ${serviceInfo.host}:${serviceInfo.port}")
                                onServiceFound(serviceInfo)
                            }
                        })
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e("P2PDiscovery", "Service lost: $service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i("P2PDiscovery", "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("P2PDiscovery", "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("P2PDiscovery", "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    /**
     * Stops broadcasting and discovery.
     */
    fun stopAll() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e("P2PDiscovery", "Error unregistering service", e)
            }
        }
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e("P2PDiscovery", "Error stopping discovery", e)
            }
        }
    }
}

