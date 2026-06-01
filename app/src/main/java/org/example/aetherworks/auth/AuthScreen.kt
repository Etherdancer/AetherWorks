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
    var currentInput by remember { mutableStateOf("") }

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
                text = "Enter your Master Password to unlock the Gatekeeper.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (isAuthenticating) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Deriving encryption keys... This may take a moment to prevent brute-force attacks.")
            } else {
                SecureKeyboard(
                    currentPassword = currentInput,
                    onPasswordChange = { currentInput = it },
                    onSubmit = {
                        val password = currentInput
                        isAuthenticating = true
                        authError = null
                        
                        coroutineScope.launch {
                            try {
                                val derivedKey = withContext(Dispatchers.IO) {
                                    val dummySalt = ByteArray(16) { 1 } 
                                    gatekeeperAgent.deriveKeyFromPassword(password.toCharArray(), dummySalt)
                                }
                                
                                currentInput = ""
                                onAuthSuccess(derivedKey)
                                
                            } catch (e: Exception) {
                                authError = "Authentication failed."
                                isAuthenticating = false
                                currentInput = ""
                            }
                        }
                    }
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
