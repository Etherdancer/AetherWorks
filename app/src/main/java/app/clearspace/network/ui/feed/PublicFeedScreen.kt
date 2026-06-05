package app.clearspace.network.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.clearspace.network.discovery.SortMode
import app.clearspace.network.storage.db.entity.ContentUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun PublicFeedScreen(modifier: Modifier = Modifier, viewModel: PublicFeedViewModel = viewModel()) {
    val feedContent by viewModel.feedContent.collectAsState()
    val isAndFilter by viewModel.indexer.isAndFilter.collectAsState()
    val sortMode by viewModel.indexer.sortMode.collectAsState()
    
    // We only expose a simplified UI here, a real app would have chips for categories
    var showFilters by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Public Feed", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { showFilters = !showFilters }) {
                Text("Filters")
            }
        }
        
        if (showFilters) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sort Mode")
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = sortMode == SortMode.CHRONOLOGICAL, onClick = { viewModel.indexer.setSortMode(SortMode.CHRONOLOGICAL) }, label = { Text("New") })
                        FilterChip(selected = sortMode == SortMode.REPUTATION, onClick = { viewModel.indexer.setSortMode(SortMode.REPUTATION) }, label = { Text("Top") })
                    }
                    
                    Text("Filter Logic", modifier = Modifier.padding(top = 8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = isAndFilter, onClick = { viewModel.indexer.setFilterMode(true) }, label = { Text("AND") })
                        FilterChip(selected = !isAndFilter, onClick = { viewModel.indexer.setFilterMode(false) }, label = { Text("OR") })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (feedContent.isEmpty()) {
            Text("No content found.", modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(feedContent) { unit ->
                    ContentCard(unit)
                }
            }
        }
    }
}

@Composable
fun ContentCard(unit: ContentUnit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(unit.title, style = MaterialTheme.typography.titleLarge)
            Text("By ${unit.authorAlias}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(unit.body, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                Text(format.format(Date(unit.timestamp)), style = MaterialTheme.typography.labelSmall)
                Text("Reputation: ${unit.likeTokens.size - unit.dislikeTokens.size}", style = MaterialTheme.typography.labelSmall)
                
                var showReportDialog by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                
                TextButton(onClick = { showReportDialog = true }) {
                    Text("Report", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                
                if (showReportDialog) {
                    AlertDialog(
                        onDismissRequest = { showReportDialog = false },
                        title = { Text("Report Content") },
                        text = { Text("Are you sure you want to report this content for violating community guidelines? This will send the content hash to the moderation queue.") },
                        confirmButton = {
                            TextButton(
                                onClick = { 
                                    coroutineScope.launch {
                                        app.clearspace.network.moderation.ContentReporter(context).reportContent(unit.contentHash, "User Reported via UI")
                                    }
                                    showReportDialog = false 
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Submit Report")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}
