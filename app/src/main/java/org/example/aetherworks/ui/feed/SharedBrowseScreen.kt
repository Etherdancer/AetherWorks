package org.example.aetherworks.ui.feed

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.PlayerView
import org.example.aetherworks.discovery.ContentHeader
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.utilities.media.MediaPlayerAgent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedBrowseScreen(
    modifier: Modifier = Modifier,
    viewModel: SharedBrowseViewModel,
    mediaPlayerAgent: MediaPlayerAgent
) {
    val headers by viewModel.sharedHeaders.collectAsState()
    val loading by viewModel.loadingHeader.collectAsState()
    val viewingContent by viewModel.viewingContent.collectAsState()

    if (viewingContent != null) {
        ContentDetailOverlay(
            content = viewingContent!!,
            onClose = {
                mediaPlayerAgent.stop()
                viewModel.clearViewingContent()
            },
            onSave = {
                viewModel.saveToPrivateLibrary(viewingContent!!)
                // Could show a toast here
            },
            mediaPlayerAgent = mediaPlayerAgent
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = { TopAppBar(title = { Text("Nearby Shared Content") }) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val searchQuery by viewModel.searchQuery.collectAsState()
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        label = { Text("Smart Scan (Filter local headers)") },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                    
                    val filteredHeaders = headers.filter { 
                        it.second.title.contains(searchQuery, ignoreCase = true) ||
                        it.second.authorAlias.contains(searchQuery, ignoreCase = true) ||
                        it.second.categoryFlags.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredHeaders.isEmpty()) {
                        Text(
                            "No shared content matches your search, or sharing is not enabled on nearby devices.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredHeaders) { (ipPort, header) ->
                                HeaderCard(ipPort = ipPort, header = header) {
                                    viewModel.openContent(ipPort, header.contentHash)
                                }
                            }
                        }
                    }
                }

                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun HeaderCard(ipPort: String, header: ContentHeader, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(header.title, style = MaterialTheme.typography.titleMedium)
            Text("By ${header.authorAlias} • From $ipPort", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            Text("Published: ${sdf.format(Date(header.timestamp))}", style = MaterialTheme.typography.bodySmall)
            Text("Reputation: ${header.reputationScore}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailOverlay(
    content: ContentUnit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    mediaPlayerAgent: MediaPlayerAgent
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content.title) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Download, contentDescription = "Save to Private Library")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("By ${content.authorAlias}", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(content.body, style = MaterialTheme.typography.bodyLarge)
            
            if (content.videoPath != null) {
                Spacer(modifier = Modifier.height(16.dp))
                VideoPlayerComponent(videoUri = content.videoPath, mediaPlayerAgent = mediaPlayerAgent)
            }
        }
    }
}

@Composable
fun VideoPlayerComponent(videoUri: String, mediaPlayerAgent: MediaPlayerAgent) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val player = mediaPlayerAgent.initializePlayer()
        onDispose {
            mediaPlayerAgent.stop()
        }
    }

    Card(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!isPlaying) {
                Button(onClick = {
                    mediaPlayerAgent.playSecureMedia(Uri.parse(videoUri))
                    isPlaying = true
                }) {
                    Text("Play Video")
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = mediaPlayerAgent.initializePlayer()
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
