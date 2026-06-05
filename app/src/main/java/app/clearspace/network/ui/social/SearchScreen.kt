package app.clearspace.network.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import app.clearspace.network.discovery.GlobalDiscoveryAgent
import app.clearspace.network.discovery.GlobalProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val discoveryAgent = remember { GlobalDiscoveryAgent(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GlobalProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Search") },
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
            Text(
                "Search for peers globally by their Alias. Only users who have explicitly opted into global discoverability will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Enter Exact Alias") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            isSearching = true
                            coroutineScope.launch {
                                searchResults = discoveryAgent.searchByAlias(searchQuery)
                                isSearching = false
                            }
                        }
                    },
                    enabled = !isSearching
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Text("No users found with that alias.", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults) { profile ->
                        GlobalProfileCard(profile = profile, onPing = {
                            coroutineScope.launch {
                                discoveryAgent.pingUser(profile.id)
                                // Show some feedback in a real app (snackbar)
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalProfileCard(profile: GlobalProfile, onPing: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder for Avatar
            Box(modifier = Modifier.size(50.dp)) {
                Icon(Icons.Filled.Search, contentDescription = "Avatar", modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.alias, style = MaterialTheme.typography.titleMedium)
                if (profile.bioSnippet.isNotBlank()) {
                    Text(profile.bioSnippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("ID: ${profile.id.take(8)}...", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onPing) {
                Text("Ping")
            }
        }
    }
}
