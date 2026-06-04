package app.clearspace.network.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.clearspace.network.storage.db.entity.ContentUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphViewScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit, viewModel: LibraryViewModel = viewModel()) {
    val libraryContent by viewModel.libraryContent.collectAsState()
    
    // Filter to private notes
    val privateNotes = libraryContent.filter { it.visibility == app.clearspace.network.storage.db.entity.Visibility.PRIVATE }
    
    // Extract bidirectional links [[Note Title]]
    val links = remember(privateNotes) {
        val linkMap = mutableListOf<Pair<ContentUnit, String>>()
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        privateNotes.forEach { note ->
            regex.findAll(note.body).forEach { match ->
                linkMap.add(Pair(note, match.groupValues[1]))
            }
        }
        linkMap
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Graph") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            if (privateNotes.isEmpty()) {
                Text("No private notes found.", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            } else {
                GraphCanvas(notes = privateNotes, links = links)
            }
        }
    }
}

@Composable
fun GraphCanvas(notes: List<ContentUnit>, links: List<Pair<ContentUnit, String>>) {
    // Basic force-directed graph stub (random positions for now to keep it lightweight)
    val nodePositions = remember(notes) {
        mutableStateMapOf<String, Offset>().apply {
            notes.forEach { note ->
                // Start with random positions within a 1000x1000 bounds
                put(note.contentHash, Offset(Random.nextFloat() * 800f + 100f, Random.nextFloat() * 800f + 100f))
            }
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                // In a real implementation we would hit-test nodes and move the specific node,
                // or pan the whole canvas. For this MVP, we just stub the drag.
            }
        }
    ) {
        // Draw lines for links
        links.forEach { (sourceNote, targetTitle) ->
            val sourcePos = nodePositions[sourceNote.contentHash]
            // Find target note by title (case insensitive)
            val targetNote = notes.find { it.title.equals(targetTitle, ignoreCase = true) }
            val targetPos = targetNote?.let { nodePositions[it.contentHash] }
            
            if (sourcePos != null && targetPos != null) {
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = sourcePos,
                    end = targetPos,
                    strokeWidth = 2f
                )
            }
        }
        
        // Draw nodes
        notes.forEach { note ->
            val pos = nodePositions[note.contentHash] ?: Offset.Zero
            drawCircle(
                color = primaryColor,
                radius = 20f,
                center = pos
            )
            // Draw title label
            drawContext.canvas.nativeCanvas.drawText(
                note.title,
                pos.x,
                pos.y - 25f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE // Simplified for contrast
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
