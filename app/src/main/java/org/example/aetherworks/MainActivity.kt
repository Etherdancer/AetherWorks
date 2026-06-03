package org.example.aetherworks

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import android.content.pm.PackageManager
import android.util.Log
import java.security.MessageDigest
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.security.EmulatorDetector
import org.example.aetherworks.security.GatekeeperRepository
import org.example.aetherworks.security.RootDetector
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.theme.AetherWorksTheme
import org.example.aetherworks.ui.auth.GatekeeperUiState
import org.example.aetherworks.ui.auth.GatekeeperViewModel
import org.example.aetherworks.ui.auth.LockScreen
import org.example.aetherworks.ui.auth.OnboardingScreen
import org.example.aetherworks.networking.SharingStateManager
import org.example.aetherworks.networking.SharingToggleViewModel
import org.example.aetherworks.ui.feed.SharedBrowseViewModel

class MainActivity : ComponentActivity() {
  private lateinit var gatekeeperRepo: GatekeeperRepository
  private lateinit var gatekeeperViewModel: GatekeeperViewModel
  private lateinit var sharingStateManager: SharingStateManager
  private lateinit var sharingToggleViewModel: SharingToggleViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    enableEdgeToEdge()

    try {
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        val signatures = packageInfo.signatures
        if (signatures != null && signatures.isNotEmpty()) {
            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(signatures[0].toByteArray())
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            // Only a placeholder known hash for demonstration. In a real scenario, this would be the actual release hash.
            val knownGoodHashes = listOf("known_good_hash_placeholder")
            if (!knownGoodHashes.contains(hashString)) {
                Log.e("AetherWorksSecurity", "CRITICAL WARNING: APK signature hash $hashString does not match known F-Droid or release hashes. App may be tampered with!")
            }
        }
    } catch (e: Exception) {
        Log.e("AetherWorksSecurity", "Failed to verify APK signature: ${e.message}")
    }

    val emulatorDetector = EmulatorDetector(this)
    if (emulatorDetector.isEmulator()) {
      setContent {
        AetherWorksTheme {
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.errorContainer) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                  "This app can only be used on a physical device.",
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.headlineMedium
              )
            }
          }
        }
      }
      return
    }

    val rootDetector = RootDetector()
    val isRootedOrCustom = rootDetector.isRootedOrCustomRom()
    // A warning banner should ideally be presented if isRootedOrCustom is true.
    // For now, we will let it pass as per F-Droid architecture specification.

    val keyManager = KeyManager(this)
    gatekeeperRepo = GatekeeperRepository(this, keyManager)
    gatekeeperViewModel = GatekeeperViewModel(gatekeeperRepo)
    
    sharingStateManager = SharingStateManager(this)
    sharingToggleViewModel = SharingToggleViewModel(sharingStateManager)

    setContent {
      val uiState by gatekeeperViewModel.uiState.collectAsState()

      AetherWorksTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          when (val state = uiState) {
            is GatekeeperUiState.Loading -> { } // Show empty or splash
            is GatekeeperUiState.Onboarding -> OnboardingScreen(onComplete = { gatekeeperViewModel.completeOnboarding(it) })
            is GatekeeperUiState.PromptPassword, is GatekeeperUiState.PasswordError, is GatekeeperUiState.LockedOut -> {
              LockScreen(uiState = state, onSubmitPassword = { gatekeeperViewModel.submitPassword(it) })
            }
            is GatekeeperUiState.Authenticated, GatekeeperUiState.Active -> {
                if (state is GatekeeperUiState.Authenticated) {
                    // Initialize databases with the real key
                    val privateDb = AetherDatabase.getPrivateDatabase(this@MainActivity, state.dbKey)
                    val sharedDb = AetherDatabase.getSharedDatabase(this@MainActivity, state.dbKey)
                    gatekeeperViewModel.clearDbKey()
                }
                
                val sharedBrowseViewModel = SharedBrowseViewModel(this@MainActivity.application)

                MainNavigation(
                    sharingToggleViewModel = sharingToggleViewModel,
                    sharedBrowseViewModel = sharedBrowseViewModel
                )
            }
          }
        }
      }
    }
  }
}
