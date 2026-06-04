package app.clearspace.network.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.clearspace.network.ui.feed.ContentCard

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreate: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToMediaVault: () -> Unit,
    onNavigateToGraph: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val libraryContent by viewModel.libraryContent.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Library", style = MaterialTheme.typography.headlineMedium)
                Column(horizontalAlignment = Alignment.End) {
                    Button(onClick = onNavigateToCreate) {
                        Text("Create Content")
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onNavigateToProfile) {
                                    Text("Profile")
                                }
                                Button(onClick = onNavigateToAbout) {
                                    Text("About")
                                }
                            }
                            Button(onClick = onNavigateToMediaVault, modifier = Modifier.padding(top = 4.dp)) {
                                Text("Media Vault")
                            }
                            Button(onClick = onNavigateToGraph, modifier = Modifier.padding(top = 4.dp)) {
                                Text("Graph View")
                            }
                        }
                    }
                }
            }
        }
        
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.loadContent(it)
                },
                label = { Text("Smart Scan") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            )
        }

        if (libraryContent.isEmpty()) {
            item {
                Text("No private or trusted content found.", modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            val allTags = libraryContent
                .filter { it.visibility == app.clearspace.network.storage.db.entity.Visibility.PRIVATE }
                .flatMap { extractTags(it.body) }
                .toSet()
                .toList()
                .sorted()
                
            var selectedTag by remember { mutableStateOf<String?>(null) }
            
            if (allTags.isNotEmpty()) {
                item {
                    Text("Tags", style = MaterialTheme.typography.titleSmall)
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allTags.forEach { tag ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedTag == tag,
                                onClick = { 
                                    selectedTag = if (selectedTag == tag) null else tag
                                },
                                label = { Text("#$tag") }
                            )
                        }
                    }
                }
            }

            val filteredContent = if (selectedTag != null) {
                libraryContent.filter { it.body.contains("#$selectedTag", ignoreCase = true) }
            } else {
                libraryContent
            }

            items(filteredContent) { unit ->
                ContentCard(unit)
            }
        }
    }
}

private fun extractTags(text: String): List<String> {
    val regex = Regex("#([a-zA-Z0-9_]+)")
    return regex.findAll(text).map { it.groupValues[1] }.toList()
}
