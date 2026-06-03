package org.example.aetherworks

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.example.aetherworks.networking.SharingToggleViewModel
import org.example.aetherworks.ui.components.GlobalSharingToggle
import org.example.aetherworks.ui.feed.SharedBrowseScreen
import org.example.aetherworks.ui.feed.SharedBrowseViewModel
import org.example.aetherworks.ui.social.SocialScreen
import org.example.aetherworks.ui.social.GroupsScreen
import org.example.aetherworks.ui.library.LibraryScreen
import org.example.aetherworks.ui.content.CreateContentScreen
import org.example.aetherworks.ui.profile.ProfileScreen
import org.example.aetherworks.ui.trust.RemoteLinkExchangeScreen
import org.example.aetherworks.ui.trust.TrustVerificationScreen
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun MainNavigation(
    sharingToggleViewModel: SharingToggleViewModel,
    sharedBrowseViewModel: SharedBrowseViewModel,
    initialDeepLinkTitle: String? = null
) {
  val backStack = rememberNavBackStack(FeedTab)
  
  androidx.compose.runtime.LaunchedEffect(initialDeepLinkTitle) {
      if (initialDeepLinkTitle != null) {
          backStack.add(CreateContent(initialDeepLinkTitle))
      }
  }
  val isSharingEnabled by sharingToggleViewModel.isSharingEnabled.collectAsState()

  // Find current tab
  val currentKey = backStack.lastOrNull() ?: FeedTab
  val isMainTab = currentKey == FeedTab || currentKey == SocialTab || currentKey == LibraryTab

  Scaffold(
      bottomBar = {
              if (isMainTab) {
                  NavigationBar {
                      NavigationBarItem(
                      selected = currentKey == FeedTab,
                      onClick = { backStack.add(FeedTab) },
                      icon = { Icon(Icons.Filled.Public, contentDescription = "Feed") },
                      label = { Text("Feed") }
                  )
                  NavigationBarItem(
                      selected = currentKey == SocialTab,
                      onClick = { backStack.add(SocialTab) },
                      icon = { Icon(Icons.Filled.Person, contentDescription = "Social") },
                      label = { Text("Social") }
                  )
                  NavigationBarItem(
                      selected = currentKey == LibraryTab,
                      onClick = { backStack.add(LibraryTab) },
                      icon = { Icon(Icons.Filled.List, contentDescription = "Library") },
                      label = { Text("Library") }
                  )
                  }
              }
      },
      floatingActionButton = {
          GlobalSharingToggle(
              isSharingEnabled = isSharingEnabled,
              onEnableSharing = { sharingToggleViewModel.enableSharing() },
              onDisableSharing = { sharingToggleViewModel.disableSharing() }
          )
      }
  ) { paddingValues ->
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
          entryProvider {
            entry<FeedTab> {
              SharedBrowseScreen(modifier = Modifier.padding(paddingValues), viewModel = sharedBrowseViewModel)
            }
            entry<SocialTab> {
              SocialScreen(
                  modifier = Modifier.padding(paddingValues),
                  onNavigateToGroups = { backStack.add(ManageGroups) },
                  onNavigateToTrust = { backStack.add(RemoteLinkExchange) }
              )
            }
            entry<LibraryTab> {
              LibraryScreen(
                  modifier = Modifier.padding(paddingValues),
                  onNavigateToCreate = { backStack.add(CreateContent()) },
                  onNavigateToProfile = { backStack.add(ProfileSettings) },
                  onNavigateToAbout = { backStack.add(AboutSettings) }
              )
            }
            entry<CreateContent> { key ->
              CreateContentScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() },
                  initialTitle = key.prefillTitle ?: ""
              )
            }
            entry<ProfileSettings> {
              ProfileScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() }
              )
            }
            entry<AboutSettings> {
              org.example.aetherworks.ui.about.AboutScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() }
              )
            }
            entry<ManageGroups> {
              GroupsScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onBack = { backStack.removeLastOrNull() }
              )
            }
            entry<RemoteLinkExchange> {
              RemoteLinkExchangeScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onBack = { backStack.removeLastOrNull() },
                  onNavigateToScanner = { backStack.add(TrustVerification) }
              )
            }
            entry<TrustVerification> {
              val context = LocalContext.current
              TrustVerificationScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onBack = { backStack.removeLastOrNull() },
                  onTokenScanned = { token -> 
                      try {
                          val uri = android.net.Uri.parse(token)
                          val pkBase64 = uri.getQueryParameter("pk")
                          val rToken = uri.getQueryParameter("token")
                          val sigBase64 = uri.getQueryParameter("sig")
                          
                          if (pkBase64 != null && rToken != null && sigBase64 != null) {
                              val pkBytes = android.util.Base64.decode(pkBase64, android.util.Base64.URL_SAFE)
                              val sigBytes = android.util.Base64.decode(sigBase64, android.util.Base64.URL_SAFE)
                              val keyManager = org.example.aetherworks.crypto.KeyManager(context)
                              val isValid = keyManager.verifySignature(rToken.toByteArray(Charsets.UTF_8), sigBytes, pkBytes)
                              
                              if (isValid) {
                                  val db = org.example.aetherworks.storage.db.AetherDatabase.getPrivateDatabase()
                                  CoroutineScope(Dispatchers.IO).launch {
                                      db.peerDao().insert(
                                          org.example.aetherworks.storage.db.entity.KnownPeer(
                                              publicKeyBase64 = pkBase64,
                                              alias = "Trusted Peer",
                                              avatarIndex = 0,
                                              onionAddress = null,
                                              trustLevel = org.example.aetherworks.storage.db.entity.TrustLevel.TRUSTED_IN_PERSON
                                          )
                                      )
                                      withContext(Dispatchers.Main) {
                                          android.widget.Toast.makeText(context, "Trust Established!", android.widget.Toast.LENGTH_SHORT).show()
                                      }
                                  }
                              } else {
                                  android.widget.Toast.makeText(context, "Invalid Signature!", android.widget.Toast.LENGTH_SHORT).show()
                              }
                          }
                      } catch (e: Exception) {
                          android.widget.Toast.makeText(context, "Invalid QR Code", android.widget.Toast.LENGTH_SHORT).show()
                      }
                      backStack.removeLastOrNull() 
                  }
              )
            }
          },
      )
  }
}
