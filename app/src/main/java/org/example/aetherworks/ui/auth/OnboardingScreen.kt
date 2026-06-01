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
import org.example.aetherworks.auth.SecurePinPad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit
) {
    var acceptedRisk by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(1) } // 1: Create, 2: Confirm
    var firstPin by remember { mutableStateOf(CharArray(0)) }
    var showError by remember { mutableStateOf(false) }

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
                    Text("• You use this software entirely at your own risk.")
                    Text("• This app runs on physical devices only.")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = acceptedRisk,
                    onCheckedChange = { acceptedRisk = it }
                )
                Text("I understand and accept all risks.")
            }

            AnimatedVisibility(
                visible = acceptedRisk,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (step == 1) "Create Master PIN" else "Confirm Master PIN",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    SecurePinPad(
                        onPinComplete = { pin ->
                            if (step == 1) {
                                firstPin = pin
                                step = 2
                                showError = false
                            } else {
                                if (firstPin.contentEquals(pin)) {
                                    onComplete(String(pin))
                                } else {
                                    showError = true
                                    step = 1
                                    firstPin = CharArray(0)
                                }
                            }
                        },
                        pinLength = 6,
                        randomizeLayout = true
                    )

                    if (showError) {
                        Text(
                            text = "PINs did not match. Please try again.",
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
