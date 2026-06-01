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
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import org.example.aetherworks.utilities.media.PlaybackState
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
import org.example.aetherworks.ui.library.LibraryScreen
import org.example.aetherworks.ui.content.CreateContentScreen
import org.example.aetherworks.ui.profile.ProfileScreen
import org.example.aetherworks.ui.utilities.UtilitiesScreen

import org.example.aetherworks.security.guard.ClipboardCleaner
import org.example.aetherworks.ui.utilities.*
import org.example.aetherworks.ui.utilities.CalendarViewModel
import org.example.aetherworks.ui.utilities.TasksViewModel
import org.example.aetherworks.ui.utilities.MediaViewModel
import org.example.aetherworks.ui.utilities.ShoppingViewModel
import org.example.aetherworks.ui.utilities.CookbookViewModel
import org.example.aetherworks.ui.utilities.ShoppingListScreen
import org.example.aetherworks.ui.utilities.CookbookScreen
import org.example.aetherworks.ui.utilities.OfflineMapsScreen
import org.example.aetherworks.ui.utilities.TranslatorScreen

@Composable
fun MainNavigation(
    sharingToggleViewModel: SharingToggleViewModel,
    vaultViewModel: VaultViewModel,
    calendarViewModel: CalendarViewModel,
    tasksViewModel: TasksViewModel,
    mediaViewModel: MediaViewModel,
    shoppingViewModel: ShoppingViewModel,
    cookbookViewModel: CookbookViewModel,
    sharedBrowseViewModel: SharedBrowseViewModel,
    clipboardCleaner: ClipboardCleaner
) {
  val backStack = rememberNavBackStack(FeedTab)
  val isSharingEnabled by sharingToggleViewModel.isSharingEnabled.collectAsState()

  // Find current tab
  val currentKey = backStack.lastOrNull() ?: FeedTab
  val isMainTab = currentKey == FeedTab || currentKey == SocialTab || currentKey == LibraryTab || currentKey == UtilitiesTab

  Scaffold(
      bottomBar = {
          Column {
              val playbackState by mediaViewModel.playbackState.collectAsState()
              val currentMedia by mediaViewModel.currentMediaItem.collectAsState()
              if (playbackState != PlaybackState.IDLE && playbackState != PlaybackState.ERROR && currentMedia != null) {
                  Surface(
                      color = MaterialTheme.colorScheme.secondaryContainer,
                      modifier = Modifier
                          .fillMaxWidth()
                          .clickable { backStack.add(MediaPlayerScreen) }
                  ) {
                      Row(
                          verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                      ) {
                          Text(
                              text = currentMedia?.fileName ?: "Unknown Media",
                              style = MaterialTheme.typography.bodyMedium,
                              modifier = Modifier.weight(1f),
                              maxLines = 1,
                              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                          )
                          IconButton(onClick = {
                              if (playbackState == PlaybackState.PLAYING) mediaViewModel.pause()
                              else mediaViewModel.resume()
                          }) {
                              Icon(
                                  imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                  contentDescription = "Play/Pause"
                              )
                          }
                      }
                  }
              }
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
          } // Closes Column
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
              SharedBrowseScreen(modifier = Modifier.padding(paddingValues), viewModel = sharedBrowseViewModel, mediaPlayerAgent = mediaViewModel.mediaPlayerAgent)
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
                  onNavigateToMedia = { backStack.add(MediaPlayerScreen) },
                  onNavigateToShopping = { backStack.add(ShoppingListScreen) },
                  onNavigateToCookbook = { backStack.add(CookbookScreen) },
                  onNavigateToMaps = { backStack.add(OfflineMapsScreen) },
                  onNavigateToTranslator = { backStack.add(TranslatorScreen) }
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
            entry<VaultScreen> { VaultScreen(modifier = Modifier.padding(paddingValues), viewModel = vaultViewModel, clipboardCleaner = clipboardCleaner) }
            entry<CalendarScreen> { CalendarScreen(modifier = Modifier.padding(paddingValues), viewModel = calendarViewModel) }
            entry<TasksScreen> { TasksScreen(modifier = Modifier.padding(paddingValues), viewModel = tasksViewModel) }
            entry<MediaPlayerScreen> { MediaPlayerScreen(modifier = Modifier.padding(paddingValues), viewModel = mediaViewModel) }
            entry<ShoppingListScreen> { ShoppingListScreen(modifier = Modifier.padding(paddingValues), viewModel = shoppingViewModel) }
            entry<CookbookScreen> { CookbookScreen(modifier = Modifier.padding(paddingValues), viewModel = cookbookViewModel) }
            entry<OfflineMapsScreen> { OfflineMapsScreen(modifier = Modifier.padding(paddingValues)) }
            entry<TranslatorScreen> { TranslatorScreen(modifier = Modifier.padding(paddingValues)) }
          },
      )
  }
}
