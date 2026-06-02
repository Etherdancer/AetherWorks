package org.example.aetherworks.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.example.aetherworks.auth.SecureKeyboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit
) {
    var acceptedRisk by remember { mutableStateOf(false) }
    var acceptedLiability by remember { mutableStateOf(false) }
    var acceptedPermanence by remember { mutableStateOf(false) }
    var acceptedAge by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(1) } // 1: Create, 2: Confirm
    var firstPassword by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%&\\-\\+()\\*\"':;!?]).{8,}\$")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to AetherWorks") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = "Important Disclaimer",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("• This app does not collect any personal data.")
                    Text("• Profiles are entirely fictional — do not use your real name or personal identity.")
                    Text("• The developer is not responsible for any consequences, data loss, or interactions resulting from using this app.")
                    Text("• You are explicitly and solely liable for any pirated or illegal content you choose to distribute.")
                    Text("• Due to the decentralized P2P network, content made public CANNOT be permanently deleted. Once public, always public.")
                    Text("• This app runs on physical devices only.")
                    Text("• You must be at least 18 years old to use this app.")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedRisk,
                        onCheckedChange = { acceptedRisk = it }
                    )
                    Text("I understand and accept all general risks.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedLiability,
                        onCheckedChange = { acceptedLiability = it }
                    )
                    Text("I accept direct, sole liability for sharing pirated or illegal content.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedPermanence,
                        onCheckedChange = { acceptedPermanence = it }
                    )
                    Text("I understand that public content is permanently public.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedAge,
                        onCheckedChange = { acceptedAge = it }
                    )
                    Text("I confirm that I am at least 18 years old.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            AnimatedVisibility(
                visible = acceptedRisk && acceptedLiability && acceptedPermanence && acceptedAge,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (step == 1) "Create Master Password" else "Confirm Master Password",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    if (step == 1) {
                        Text(
                            text = "Requirements: Min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 symbol.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = "WARNING: There is no 'forgot password'. If you lose this password, you will permanently lose access to all your private data.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    SecureKeyboard(
                        currentPassword = currentInput,
                        onPasswordChange = { currentInput = it },
                        onSubmit = {
                            if (step == 1) {
                                if (passwordRegex.matches(currentInput)) {
                                    firstPassword = currentInput
                                    currentInput = ""
                                    step = 2
                                    showError = false
                                } else {
                                    showError = true
                                    errorMessage = "Password does not meet requirements."
                                }
                            } else {
                                if (firstPassword == currentInput) {
                                    onComplete(currentInput)
                                } else {
                                    showError = true
                                    errorMessage = "Passwords do not match. Try again."
                                    step = 1
                                    firstPassword = ""
                                    currentInput = ""
                                }
                            }
                        }
                    )

                    if (showError) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
