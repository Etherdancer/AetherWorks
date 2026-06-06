package app.clearspace.network.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.clearspace.network.auth.SecureKeyboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit
) {
    var acceptedRisk by remember { mutableStateOf(false) }
    var acceptedLiability by remember { mutableStateOf(false) }
    var acceptedModeration by remember { mutableStateOf(false) }
    var acceptedAge by remember { mutableStateOf(false) }
    var acceptedEULA by remember { mutableStateOf(false) }
    var acceptedFirebase by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(1) } // 1: Create, 2: Confirm
    var firstPassword by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%&\\-\\+()\\*\"':;!?]).{8,}\$")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Clear Space") },
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
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Welcome & Privacy Overview",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("• This app does not collect any personal data.")
                    Text("• Profiles are entirely fictional — do not use your real name or personal identity.")
                    Text("• You are in control of your own data and interactions.")
                    Text("• Please be responsible. You are accountable for the content you choose to share.")
                    Text("• Public content is subject to community moderation and may be removed from the network if it violates safety guidelines.")
                    Text("• This app runs on physical devices only.")
                    Text("• You must be at least 12 years old to use this app.")
                    Text("• You agree to not upload abusive, illegal, or objectional content, as outlined in the Terms of Service.")
                    Text("• This app utilizes the absolute minimum tracking necessary (via Firebase) because remote content moderation is mandatory to comply with Google Play Store policies and GDPR.")
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
                    Text("I accept responsibility for the content I share.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedModeration,
                        onCheckedChange = { acceptedModeration = it }
                    )
                    Text("I understand that public content is subject to moderation and removal.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedAge,
                        onCheckedChange = { acceptedAge = it }
                    )
                    Text("I confirm that I am at least 12 years old.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedEULA,
                        onCheckedChange = { acceptedEULA = it }
                    )
                    Text("I agree to the Terms of Service and will not upload abusive content.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedFirebase,
                        onCheckedChange = { acceptedFirebase = it }
                    )
                    Text("I understand that Firebase uses minimal tracking for mandatory content moderation and compliance.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            AnimatedVisibility(
                visible = acceptedRisk && acceptedLiability && acceptedModeration && acceptedAge && acceptedEULA && acceptedFirebase,
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
                            text = "Note: For your privacy, there is no 'forgot password' feature. Please keep your password safe to avoid losing access to your data.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
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

