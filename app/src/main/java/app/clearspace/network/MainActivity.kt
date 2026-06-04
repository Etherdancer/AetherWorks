package app.clearspace.network

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
import app.clearspace.network.crypto.KeyManager
import app.clearspace.network.security.EmulatorDetector
import app.clearspace.network.security.GatekeeperRepository
import app.clearspace.network.security.RootDetector
import app.clearspace.network.storage.db.AetherDatabase
import app.clearspace.network.theme.AetherWorksTheme
import app.clearspace.network.ui.auth.GatekeeperUiState
import app.clearspace.network.ui.auth.GatekeeperViewModel
import app.clearspace.network.ui.auth.LockScreen
import app.clearspace.network.ui.auth.OnboardingScreen
import app.clearspace.network.networking.SharingStateManager
import app.clearspace.network.networking.SharingToggleViewModel
import app.clearspace.network.ui.feed.SharedBrowseViewModel

class MainActivity : ComponentActivity() {
  private lateinit var gatekeeperRepo: GatekeeperRepository
  private lateinit var gatekeeperViewModel: GatekeeperViewModel
  private lateinit var sharingStateManager: SharingStateManager
  private lateinit var sharingToggleViewModel: SharingToggleViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    enableEdgeToEdge()



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
                
                val deepLinkTitle = if (intent?.action == android.content.Intent.ACTION_VIEW && intent?.data?.scheme == "aetherworks") {
                    intent?.data?.getQueryParameter("title")
                } else null

                MainNavigation(
                    sharingToggleViewModel = sharingToggleViewModel,
                    sharedBrowseViewModel = sharedBrowseViewModel,
                    initialDeepLinkTitle = deepLinkTitle
                )
            }
          }
        }
      }
    }
  }
}
