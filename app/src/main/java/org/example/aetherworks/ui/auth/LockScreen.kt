package org.example.aetherworks.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.example.aetherworks.auth.SecureKeyboard

@Composable
fun LockScreen(
    uiState: GatekeeperUiState,
    onSubmitPassword: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    val isError = uiState is GatekeeperUiState.PasswordError
    val isLocked = uiState is GatekeeperUiState.LockedOut

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                modifier = Modifier.size(72.dp),
                tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AetherWorks",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter master password",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            SecureKeyboard(
                currentPassword = password,
                onPasswordChange = { password = it },
                onSubmit = {
                    if (!isLocked) {
                        onSubmitPassword(password)
                        password = ""
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = isError, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = "Incorrect password.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = isLocked, enter = fadeIn(), exit = fadeOut()) {
                if (uiState is GatekeeperUiState.LockedOut) {
                    Text(
                        text = "Too many failed attempts. Locked for ${uiState.remainingSeconds}s.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            var showWipeConfirmDialog by remember { mutableStateOf(false) }

            TextButton(
                onClick = { showWipeConfirmDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Lost Password? Wipe App Data & Reset")
            }

            if (showWipeConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showWipeConfirmDialog = false },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Wipe All Data?") },
                    text = { 
                        Text(
                            "This will permanently delete your database, profile, identity keys, and all private content. It cannot be undone. Are you sure?",
                            color = MaterialTheme.colorScheme.error
                        ) 
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // TODO: Call actual wipe data function (Runtime.getRuntime().exec("pm clear org.example.aetherworks"))
                                // Or trigger local broadcast to Gatekeeper
                                showWipeConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Wipe Everything")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWipeConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
