package org.example.aetherworks.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
            title = { Text("Enable Data Sharing?") },
            text = { Text("You are about to enable data sharing with nearby devices. Other users of this app on the same network or in Bluetooth/Wi-Fi Direct range will be able to see content you have marked as 'Public'.\n\nWARNING: ONCE PUBLIC, ALWAYS PUBLIC. Content cannot be recalled. Do you want to continue?") },
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
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Risk Disclosure") },
            text = { Text("This app communicates with nearby devices over your local network, Bluetooth, and Wi-Fi Direct. On public networks (coffee shops, airports), other people on the same network may detect that you are running this app. The developer is not responsible for any consequences. You are solely liable for the content you share. You use this feature at your own risk.") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("I Accept All Risk")
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
