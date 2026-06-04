package app.clearspace.network

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
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
import app.clearspace.network.theme.ClearSpaceTheme
import app.clearspace.network.ui.auth.GatekeeperUiState
import app.clearspace.network.ui.auth.GatekeeperViewModel
import app.clearspace.network.ui.auth.LockScreen
import app.clearspace.network.ui.auth.OnboardingScreen
import app.clearspace.network.networking.SharingStateManager
import app.clearspace.network.networking.SharingToggleViewModel
import app.clearspace.network.ui.feed.SharedBrowseViewModel

class MainActivity : FragmentActivity() {
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
        ClearSpaceTheme {
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
      val themeManager = androidx.compose.runtime.remember { app.clearspace.network.theme.ThemeManager(this@MainActivity) }
      val currentTheme by themeManager.theme.collectAsState()
      val uiState by gatekeeperViewModel.uiState.collectAsState()

      ClearSpaceTheme(appTheme = currentTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          when (val state = uiState) {
            is GatekeeperUiState.Loading -> { } // Show empty or splash
            is GatekeeperUiState.Onboarding -> OnboardingScreen(onComplete = { gatekeeperViewModel.completeOnboarding(it) })
            is GatekeeperUiState.PromptPassword, is GatekeeperUiState.PasswordError, is GatekeeperUiState.LockedOut -> {
              LockScreen(
                  uiState = state, 
                  onSubmitPassword = { gatekeeperViewModel.submitPassword(it) },
                  onBiometricClick = {
                      if (gatekeeperViewModel.canUseBiometric()) {
                          try {
                              val cipher = gatekeeperViewModel.getBiometricCipher(javax.crypto.Cipher.DECRYPT_MODE)
                              val helper = app.clearspace.network.security.BiometricHelper(this@MainActivity, keyManager)
                              helper.showBiometricPrompt(
                                  this@MainActivity,
                                  "Unlock Clear Space",
                                  "Confirm your identity to unlock",
                                  onSuccess = { gatekeeperViewModel.authenticateWithBiometric(cipher) },
                                  onError = { /* handle error */ }
                              )
                          } catch (e: Exception) {
                              // Handle crypto init error
                          }
                      }
                  },
                  canUseBiometric = gatekeeperViewModel.canUseBiometric(),
                  onEnrollBiometric = { pass ->
                      try {
                          val cipher = gatekeeperViewModel.getBiometricCipher(javax.crypto.Cipher.ENCRYPT_MODE)
                          val helper = app.clearspace.network.security.BiometricHelper(this@MainActivity, keyManager)
                          helper.showBiometricPrompt(
                              this@MainActivity,
                              "Enable Biometric Unlock",
                              "Confirm your identity to enable biometric unlock",
                              onSuccess = { 
                                  if (gatekeeperViewModel.enrollBiometric(cipher, pass)) {
                                      gatekeeperViewModel.submitPassword(pass)
                                  } 
                              },
                              onError = { /* handle error */ }
                          )
                      } catch (e: Exception) {
                          // Handle error
                      }
                  }
              )
            }
            is GatekeeperUiState.Authenticated, GatekeeperUiState.Active -> {
                if (state is GatekeeperUiState.Authenticated) {
                    // Initialize databases with the real key
                    val privateDb = AetherDatabase.getPrivateDatabase(this@MainActivity, state.dbKey)
                    val sharedDb = AetherDatabase.getSharedDatabase(this@MainActivity, state.dbKey)
                    gatekeeperViewModel.clearDbKey()
                    
                    // UGC Compliance: Sync blacklist from Firebase
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        app.clearspace.network.moderation.ModerationAgent(this@MainActivity).syncBlacklist()
                    }
                }
                
                val sharedBrowseViewModel = SharedBrowseViewModel(this@MainActivity.application)
                
                val deepLinkTitle = if (intent?.action == android.content.Intent.ACTION_VIEW && intent?.data?.scheme == "ClearSpace") {
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

