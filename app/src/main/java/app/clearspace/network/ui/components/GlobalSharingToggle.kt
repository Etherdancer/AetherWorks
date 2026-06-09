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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

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

    IconButton(
        onClick = {
            if (isSharingEnabled) {
                onDisableSharing()
            } else {
                showConsentDialog = true
            }
        },
        modifier = modifier
    ) {
        if (isSharingEnabled) {
            RadarAnimation(
                modifier = Modifier.size(24.dp),
                color = androidx.compose.ui.graphics.Color.Green
            )
        } else {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Start Sharing"
            )
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Start Sharing Data?") },
            text = { Text("You are about to enable data sharing. 'Public' content will be shared with nearby devices AND securely synced over the internet with your Remote Trusted Users. 'Trusted' content will securely sync over the internet only with authorized remote contacts. Your private data is completely isolated and stays entirely on your device. Do you want to continue?\n\nNote: Once public, content propagates to others and cannot be recalled. We use Firebase to receive background wake-up pings and for a centralized moderation blacklist. This minimal tracking is strictly necessary to publish on the Google Play Store and to satisfy laws and regulations regarding content moderation.") },
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
            text = { Text("This app communicates with nearby devices over local networks (Bluetooth/Wi-Fi Direct) and syncs both Public and Trusted content globally over the internet with your Remote Contacts. On public networks, other people may detect that you are running this app. Because we use Firebase for essential background connectivity and moderation, Google servers will know your device is running this app.\n\nWe use minimal tracking solely for legal and store compliance. You retain complete control over what leaves your device. You are responsible for the content you choose to share. You use this feature at your own risk.") },
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
