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
import org.example.aetherworks.ui.feed.PublicFeedScreen
import org.example.aetherworks.ui.social.SocialScreen
import org.example.aetherworks.ui.library.LibraryScreen
import org.example.aetherworks.ui.content.CreateContentScreen
import org.example.aetherworks.ui.profile.ProfileScreen
import org.example.aetherworks.ui.utilities.UtilitiesScreen

@Composable
fun MainNavigation(sharingToggleViewModel: SharingToggleViewModel) {
  val backStack = rememberNavBackStack(FeedTab)
  val isSharingEnabled by sharingToggleViewModel.isSharingEnabled.collectAsState()

  // Find current tab
  val currentKey = backStack.lastOrNull() ?: FeedTab
  val isMainTab = currentKey == FeedTab || currentKey == SocialTab || currentKey == LibraryTab || currentKey == UtilitiesTab

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
                  NavigationBarItem(
                      selected = currentKey == UtilitiesTab,
                      onClick = { backStack.add(UtilitiesTab) },
                      icon = { Icon(Icons.Filled.Build, contentDescription = "Utilities") },
                      label = { Text("Tools") }
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
              PublicFeedScreen(modifier = Modifier.padding(paddingValues))
            }
            entry<SocialTab> {
              SocialScreen(modifier = Modifier.padding(paddingValues))
            }
            entry<LibraryTab> {
              LibraryScreen(
                  modifier = Modifier.padding(paddingValues),
                  onNavigateToCreate = { backStack.add(CreateContent) },
                  onNavigateToProfile = { backStack.add(ProfileSettings) }
              )
            }
            entry<UtilitiesTab> {
              UtilitiesScreen(
                  modifier = Modifier.padding(paddingValues),
                  onNavigateToVault = { backStack.add(VaultScreen) },
                  onNavigateToCalendar = { backStack.add(CalendarScreen) },
                  onNavigateToTasks = { backStack.add(TasksScreen) },
                  onNavigateToMedia = { backStack.add(MediaPlayerScreen) }
              )
            }
            entry<CreateContent> {
              CreateContentScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() }
              )
            }
            entry<ProfileSettings> {
              ProfileScreen(
                  modifier = Modifier.safeDrawingPadding(),
                  onNavigateBack = { backStack.removeLastOrNull() }
              )
            }
            entry<VaultScreen> { Text("Vault Screen Placeholder", modifier = Modifier.padding(paddingValues)) }
            entry<CalendarScreen> { Text("Calendar Screen Placeholder", modifier = Modifier.padding(paddingValues)) }
            entry<TasksScreen> { Text("Tasks Screen Placeholder", modifier = Modifier.padding(paddingValues)) }
            entry<MediaPlayerScreen> { Text("Media Player Placeholder", modifier = Modifier.padding(paddingValues)) }
          },
      )
  }
}
