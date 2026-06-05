package app.clearspace.network.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.os.Build

@Composable
fun GlobalSharingToggle(
    isSharingEnabled: Boolean,
    onEnableSharing: () -> Unit,
    onDisableSharing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConsentDialog by remember { mutableStateOf(false) }
    var showRiskDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onEnableSharing()
    }

    FloatingActionButton(
        onClick = {
            if (isSharingEnabled) {
                onDisableSharing()
            } else {
                showConsentDialog = true
            }
        },
        containerColor = if (isSharingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSharingEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = if (isSharingEnabled) "Stop Sharing" else "Start Sharing"
        )
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Start Sharing Data?") },
            text = { Text("You are about to enable data sharing with nearby devices. Other users of this app on the same network or in Bluetooth/Wi-Fi Direct range will be able to see content you have marked as 'Public'.\n\nNote: Once public, content propagates to others and cannot be recalled.\n\nWe use Firebase to receive background wake-up pings, and cross-reference content with a centralized blacklist. This minimum tracking is strictly necessary for remote content moderation to comply with Google Play Store policies and GDPR.\n\nTip: To save battery, keep this toggle OFF when you are at home, and only turn it ON when you are in public spaces.") },
            confirmButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    showRiskDialog = true
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRiskDialog) {
        AlertDialog(
            onDismissRequest = { showRiskDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Privacy Overview") },
            text = { Text("This app communicates with nearby devices over your local network, Bluetooth, and Wi-Fi Direct. On public networks, other people on the same network may detect that you are running this app.\n\nBecause we use Firebase for essential background connectivity and moderation, Google servers will know your device is running this app.\n\nYou are responsible for the content you share. By continuing, you acknowledge these conditions.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRiskDialog = false
                        val permissions = buildList {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(android.Manifest.permission.POST_NOTIFICATIONS)
                                add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                add(android.Manifest.permission.BLUETOOTH_SCAN)
                                add(android.Manifest.permission.BLUETOOTH_CONNECT)
                                add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
                            }
                        }
                        
                        val ungranted = permissions.filter {
                            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                        
                        if (ungranted.isEmpty()) {
                            onEnableSharing()
                        } else {
                            permissionLauncher.launch(ungranted.toTypedArray())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Acknowledge & Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRiskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
