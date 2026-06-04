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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("My Library", style = MaterialTheme.typography.headlineMedium)
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Button(onClick = onNavigateToCreate) {
                        Text("Create Content")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onNavigateToProfile) {
                            Text("Profile")
                        }
                        Button(onClick = onNavigateToAbout) {
                            Text("About")
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
            items(libraryContent) { unit ->
                ContentCard(unit)
            }
        }
    }
}
