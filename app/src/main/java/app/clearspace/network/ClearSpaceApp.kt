package app.clearspace.network

import android.app.Application

class ClearSpaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase and App Check for UGC compliance and security
        com.google.firebase.FirebaseApp.initializeApp(this)
        com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}

