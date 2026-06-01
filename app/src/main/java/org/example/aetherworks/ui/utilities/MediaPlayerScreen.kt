package org.example.aetherworks.ui.utilities

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.aetherworks.storage.db.entity.MediaItem
import org.example.aetherworks.utilities.media.PlaybackState

import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import java.io.File
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaViewModel
) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentMedia by viewModel.currentMediaItem.collectAsState()

    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    var lyricsText by remember { mutableStateOf("Searching for .lrc files locally...\n\n(Lyrics would scroll here)") }

    LaunchedEffect(currentMedia) {
        currentMedia?.let { media ->
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, Uri.parse(media.filePath))
                val artBytes = mmr.embeddedPicture
                if (artBytes != null) {
                    albumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                } else {
                    albumArt = null
                }
                mmr.release()

                // Basic LRC check (assuming local file path)
                val path = Uri.parse(media.filePath).path
                if (path != null) {
                    val lrcFile = File(path.substringBeforeLast(".") + ".lrc")
                    if (lrcFile.exists()) {
                        lyricsText = lrcFile.readText()
                    } else {
                        lyricsText = "No embedded lyrics or .lrc file found."
                    }
                }
            } catch (e: Exception) {
                albumArt = null
                lyricsText = "Error loading metadata."
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Document picker returns a URI we have read access to
            viewModel.playMedia(it.toString(), "Selected Media")
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Media Player") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch(arrayOf("audio/*", "video/*")) }) {
                Icon(Icons.Default.Audiotrack, contentDescription = "Open Media")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Current Playback controls
            if (playbackState != PlaybackState.IDLE && playbackState != PlaybackState.ERROR) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    var sleepTimerExpanded by remember { mutableStateOf(false) }
                    var sleepTimerActive by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentMedia?.fileName ?: if (playbackState == PlaybackState.PLAYING) "Playing" else "Paused",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                IconButton(onClick = { sleepTimerExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Sleep Timer",
                                        tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = sleepTimerExpanded,
                                    onDismissRequest = { sleepTimerExpanded = false }
                                ) {
                                    val timerOptions = listOf(5, 15, 30, 60)
                                    timerOptions.forEach { minutes ->
                                        DropdownMenuItem(
                                            text = { Text("$minutes minutes") },
                                            onClick = {
                                                viewModel.setSleepTimer(minutes)
                                                sleepTimerActive = true
                                                sleepTimerExpanded = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Cancel Timer") },
                                        onClick = {
                                            viewModel.cancelSleepTimer()
                                            sleepTimerActive = false
                                            sleepTimerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        albumArt?.let { bitmap ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var currentSpeed by remember(currentMedia) { mutableStateOf(currentMedia?.playbackSpeed ?: 1.0f) }
                            Text("Speed: ${String.format("%.1fx", currentSpeed)}")
                            Slider(
                                value = currentSpeed,
                                onValueChange = { currentSpeed = it },
                                onValueChangeFinished = { viewModel.setPlaybackSpeed(currentSpeed) },
                                valueRange = 0.5f..2.5f,
                                steps = 19,
                                modifier = Modifier.width(150.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { 
                                    if (playbackState == PlaybackState.PLAYING) viewModel.pause()
                                    else viewModel.resume()
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            } else if (playbackState == PlaybackState.ERROR) {
                Text(
                    text = "Playback Error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (currentMedia != null && playbackState != PlaybackState.IDLE) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f) // Takes up remaining space for scrolling lyrics
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Fast Lyrics",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Text(
                                    text = lyricsText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { media ->
                    MediaHistoryCard(
                        media = media,
                        onPlay = { viewModel.playMedia(media) }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaHistoryCard(media: MediaItem, onPlay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = media.fileName, style = MaterialTheme.typography.titleMedium)
                Text(text = media.filePath, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
