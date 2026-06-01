package org.example.aetherworks.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.aetherworks.ui.feed.ContentCard

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreate: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val libraryContent by viewModel.libraryContent.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("My Library", style = MaterialTheme.typography.headlineMedium)
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Button(onClick = onNavigateToCreate) {
                    Text("Create Content")
                }
                Button(onClick = onNavigateToProfile) {
                    Text("Edit Profile")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (libraryContent.isEmpty()) {
            Text("No private or trusted content found.", modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(libraryContent) { unit ->
                    ContentCard(unit)
                }
            }
        }
    }
}
