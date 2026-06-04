package app.clearspace.network.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaVaultScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val filesDir = context.filesDir
    
    // Find all files ending in .aether (our encrypted format)
    val encryptedFiles = remember {
        filesDir.listFiles { file -> file.name.endsWith(".aether") }?.toList() ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encrypted Media Vault") },
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "These files are encrypted at rest. They are completely isolated from your device's public media gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (encryptedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No encrypted files found in your vault.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(encryptedFiles) { file ->
                        EncryptedFileCard(file)
                    }
                }
            }
        }
    }
}

@Composable
fun EncryptedFileCard(file: File) {
    var showDecryptDialog by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(file.name, style = MaterialTheme.typography.titleMedium)
            
            val sizeMb = file.length() / (1024.0 * 1024.0)
            val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val dateStr = format.format(Date(file.lastModified()))
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(String.format("%.2f MB", sizeMb), style = MaterialTheme.typography.labelMedium)
                Text(dateStr, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showDecryptDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Decrypt & View")
            }
        }
    }
    
    if (showDecryptDialog) {
        AlertDialog(
            onDismissRequest = { showDecryptDialog = false },
            title = { Text("Decrypt File") },
            text = {
                Column {
                    Text("Enter the passphrase to decrypt this file in memory.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Decrypt logic stub
                    // In a full implementation, this uses FileEncryptionUtil.decrypt() 
                    // and opens the result in a temporary secure viewer.
                    showDecryptDialog = false
                }) {
                    Text("Decrypt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecryptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
