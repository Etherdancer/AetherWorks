package org.example.aetherworks.ui.content

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.aetherworks.storage.db.entity.Visibility
import org.example.aetherworks.utils.MediaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContentScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(Visibility.PRIVATE) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedMediaUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Content") },
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
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = 10
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Visibility")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = visibility == Visibility.PRIVATE,
                    onClick = { visibility = Visibility.PRIVATE },
                    label = { Text("Private") }
                )
                FilterChip(
                    selected = visibility == Visibility.TRUSTED,
                    onClick = { visibility = Visibility.TRUSTED },
                    label = { Text("Trusted") }
                )
                FilterChip(
                    selected = visibility == Visibility.PUBLIC,
                    onClick = { visibility = Visibility.PUBLIC },
                    label = { Text("Public") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    isVideo = false
                    mediaPicker.launch("image/*") 
                }) {
                    Text("Attach Image")
                }
                Button(onClick = { 
                    isVideo = true
                    mediaPicker.launch("video/*") 
                }) {
                    Text("Attach Video")
                }
            }
            
            if (selectedMediaUri != null) {
                Text("Media attached: ${if (isVideo) "Video" else "Image"}", color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        // Compress and save media if any
                        selectedMediaUri?.let { uri ->
                            if (isVideo) {
                                MediaUtils.copyAndSaveVideo(context, uri)
                            } else {
                                MediaUtils.compressAndSaveImage(context, uri)
                            }
                        }
                        
                        // TODO: Save to database using AetherDatabase
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
