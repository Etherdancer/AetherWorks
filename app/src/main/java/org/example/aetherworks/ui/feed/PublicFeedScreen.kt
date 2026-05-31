package org.example.aetherworks.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PublicFeedScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Public Feed", style = MaterialTheme.typography.headlineMedium)
        Text("Discovered public content will appear here.", modifier = Modifier.padding(top = 16.dp))
    }
}
