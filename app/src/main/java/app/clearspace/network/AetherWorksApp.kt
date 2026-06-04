package app.clearspace.network

import android.app.Application

class AetherWorksApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Core initialization logic for agents will reside here
        // (e.g., Gatekeeper check, DB init, Tor proxy setup)
    }
}
