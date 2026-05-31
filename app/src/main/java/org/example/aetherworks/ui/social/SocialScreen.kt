package org.example.aetherworks.ui.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SocialScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Social Network", style = MaterialTheme.typography.headlineMedium)
        Text("Discovered nearby profiles will appear here.", modifier = Modifier.padding(top = 16.dp))
    }
}
