package org.example.aetherworks.ui.utilities

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(modifier: Modifier = Modifier) {
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isModelDownloaded by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Simple dictionary for fallback offline translation demo
    val dictionary = mapOf(
        "hello" to "hola",
        "world" to "mundo",
        "friend" to "amigo",
        "privacy" to "privacidad",
        "security" to "seguridad",
        "offline" to "desconectado"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Offline Translator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isModelDownloaded) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Language Models Not Downloaded", style = MaterialTheme.typography.titleMedium)
                        Text("Download the English-Spanish dictionary pack to translate completely offline.", style = MaterialTheme.typography.bodyMedium)
                        
                        if (isDownloading) {
                            LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
                            Text("${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Button(onClick = {
                                isDownloading = true
                                coroutineScope.launch {
                                    for (i in 1..100) {
                                        delay(30)
                                        downloadProgress = i / 100f
                                    }
                                    isDownloading = false
                                    isModelDownloaded = true
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download EN-ES Pack (1.2 MB)")
                            }
                        }
                    }
                }
            } else {
                Text("Source Text (English)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("Enter text to translate...") }
                )

                Button(
                    onClick = {
                        val words = sourceText.lowercase().split(Regex("\\s+"))
                        val translated = words.joinToString(" ") { word ->
                            val cleanWord = word.replace(Regex("[^a-z]"), "")
                            val trans = dictionary[cleanWord]
                            if (trans != null) word.replace(cleanWord, trans, ignoreCase = true) else word
                        }
                        translatedText = if (sourceText.isNotBlank()) translated else ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Translate to Spanish")
                }

                Text("Translated Text", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = translatedText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}
