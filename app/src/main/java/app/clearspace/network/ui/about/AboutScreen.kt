package app.clearspace.network.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit, onNavigateToLicenses: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & Support") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("About & Support", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("For bug reports and support, please contact:", style = MaterialTheme.typography.bodyMedium)
            Text("etherdancer.zero553@aleeas.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Support the Developer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("If you like Clear Space, consider leaving a tip via Bitcoin (Silent Payments).", style = MaterialTheme.typography.bodySmall)
            
            val clipboardManager = LocalClipboardManager.current
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString("sp1qqvc4svdel9fkulcyk8xjyyyhcqzfudtnmu8xrerkaqgv7wwt0hlxzqlp6xkyfwvaxnv7p93y3ckw6trkssvj4t52tlv235lye4kqr9fnhvxxemua"))
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Copy Bitcoin Donation Address")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Legal & Compliance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            OutlinedButton(
                onClick = onNavigateToLicenses,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("View Open Source Licenses")
            }
        }
    }
}

