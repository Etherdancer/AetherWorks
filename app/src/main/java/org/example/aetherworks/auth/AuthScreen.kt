package org.example.aetherworks.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Arrays

@Composable
fun AuthScreen(
    onAuthSuccess: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val gatekeeperAgent = remember { GatekeeperAgent() }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AetherWorks",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enter your PIN to unlock the Gatekeeper.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (isAuthenticating) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Deriving encryption keys... This may take a moment to prevent brute-force attacks.")
            } else {
                SecurePinPad(
                    onPinComplete = { pin ->
                        isAuthenticating = true
                        authError = null
                        
                        coroutineScope.launch {
                            try {
                                // Offload heavy Argon2id hashing to IO dispatcher
                                val derivedKey = withContext(Dispatchers.IO) {
                                    // In a real scenario, the salt would be loaded from SharedPreferences
                                    // For this initial UI testing, we generate a fake salt.
                                    // Real implementation will query the DB/Prefs for the existing salt.
                                    val dummySalt = ByteArray(16) { 1 } 
                                    gatekeeperAgent.deriveKeyFromPassword(pin, dummySalt)
                                }
                                
                                // Clean up the pin array immediately
                                Arrays.fill(pin, '\u0000')
                                
                                onAuthSuccess(derivedKey)
                                
                            } catch (e: Exception) {
                                authError = "Authentication failed."
                                isAuthenticating = false
                            }
                        }
                    },
                    pinLength = 6,
                    randomizeLayout = true
                )
            }
            
            if (authError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = authError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
