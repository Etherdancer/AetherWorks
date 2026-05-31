package org.example.aetherworks.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(modifier: Modifier = Modifier, onNavigateToCreate: () -> Unit, onNavigateToProfile: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("My Library", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToCreate) {
            Text("Create New Content")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToProfile) {
            Text("Edit My Profile")
        }
    }
}
