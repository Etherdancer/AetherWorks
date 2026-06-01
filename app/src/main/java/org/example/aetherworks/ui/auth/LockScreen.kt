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
import org.example.aetherworks.auth.SecurePinPad

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

            SecurePinPad(
                onPinComplete = { pin ->
                    if (!isLocked) {
                        onSubmitPassword(String(pin))
                    }
                },
                pinLength = 6,
                randomizeLayout = true
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

            // Button removed as SecurePinPad submits automatically on completion
        }
    }
}
