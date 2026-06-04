package app.clearspace.network

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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.clearspace.network.networking.SharingToggleViewModel
import app.clearspace.network.ui.components.GlobalSharingToggle
import app.clearspace.network.ui.components.TofuConfirmationHost
import app.clearspace.network.ui.feed.SharedBrowseScreen
import app.clearspace.network.ui.feed.SharedBrowseViewModel
import app.clearspace.network.ui.social.SocialScreen
import app.clearspace.network.ui.social.GroupsScreen
import app.clearspace.network.ui.library.LibraryScreen
import app.clearspace.network.ui.content.CreateContentScreen
import app.clearspace.network.ui.profile.ProfileScreen
import app.clearspace.network.ui.trust.RemoteLinkExchangeScreen
import app.clearspace.network.ui.trust.TrustVerificationScreen
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
      // FIX C2: Show TOFU fingerprint confirmation dialog whenever a new peer connects
      TofuConfirmationHost()
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        enterTransition = { slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) },
        entryProvider =
          entryProvider {
            entry<FeedTab> {
              SharedBrowseScreen(
                  modifier = Modifier.padding(paddingValues), 
                  viewModel = sharedBrowseViewModel,
                  onShareToGroup = { title, body ->
                      backStack.add(CreateContent(prefillTitle = title, prefillBody = body, forceVisibilityGroup = true))
                  }
              )
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
                  onNavigateToAbout = { backStack.add(AboutSettings) },
                  onNavigateToMediaVault = { backStack.add(MediaVault) },
                  onNavigateToGraph = { backStack.add(GraphView) }
              )
            }
            entry<CreateContent> { key ->
              CreateContentScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() },
                  initialTitle = key.prefillTitle ?: "",
                  initialBody = key.prefillBody ?: "",
                  forceVisibilityGroup = key.forceVisibilityGroup
              )
            }
            entry<ProfileSettings> {
              ProfileScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() }
              )
            }
            entry<AboutSettings> {
              app.clearspace.network.ui.about.AboutScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() }
              )
            }
            entry<MediaVault> {
              app.clearspace.network.ui.library.MediaVaultScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() }
              )
            }
            entry<GraphView> {
              app.clearspace.network.ui.library.GraphViewScreen(
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
                          val rToken = uri.getQueryParameter("rt")
                          val sigBase64 = uri.getQueryParameter("sig")
                          
                          if (pkBase64 != null && rToken != null && sigBase64 != null) {
                              val pkBytes = android.util.Base64.decode(pkBase64, android.util.Base64.URL_SAFE)
                              val sigBytes = android.util.Base64.decode(sigBase64, android.util.Base64.URL_SAFE)
                              val keyManager = app.clearspace.network.crypto.KeyManager(context)
                              val isValid = keyManager.verifySignature(rToken.toByteArray(Charsets.UTF_8), sigBytes, pkBytes)
                              
                              if (isValid) {
                                  val db = app.clearspace.network.storage.db.AetherDatabase.getPrivateDatabase()
                                  CoroutineScope(Dispatchers.IO).launch {
                                      db.peerDao().insert(
                                          app.clearspace.network.storage.db.entity.KnownPeer(
                                              publicKeyBase64 = pkBase64,
                                              alias = "Trusted Peer",
                                              avatarIndex = 0,
                                              onionAddress = null,
                                              trustLevel = app.clearspace.network.storage.db.entity.TrustLevel.TRUSTED_IN_PERSON
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
            entry<ChatRoute> { key ->
                app.clearspace.network.ui.chat.ChatScreen(
                    modifier = Modifier.safeDrawingPadding(),
                    peerPublicKey = key.peerKey,
                    peerAlias = key.peerAlias,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
          },
      )
  }
}
