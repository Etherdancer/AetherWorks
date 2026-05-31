package org.example.aetherworks

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.example.aetherworks.ui.main.MainScreen
import org.example.aetherworks.ui.components.GlobalSharingToggle
import org.example.aetherworks.networking.SharingToggleViewModel

@Composable
fun MainNavigation(sharingToggleViewModel: SharingToggleViewModel) {
  val backStack = rememberNavBackStack(Main)
  val isSharingEnabled by sharingToggleViewModel.isSharingEnabled.collectAsState()

  Scaffold(
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
            entry<Main> {
              MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.safeDrawingPadding().padding(paddingValues).padding(16.dp))
            }
          },
      )
  }
}
