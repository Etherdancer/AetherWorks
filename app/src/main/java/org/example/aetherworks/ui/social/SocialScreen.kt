package org.example.aetherworks.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SocialScreen(modifier: Modifier = Modifier, viewModel: SocialViewModel = viewModel()) {
    val profiles by viewModel.nearbyProfiles.collectAsState()
    
    val acquaintances = profiles.filter { it.isAcquaintance }
    val nearby = profiles.filter { !it.isAcquaintance }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Social Network", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (acquaintances.isNotEmpty()) {
            Text("Acquaintances", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(acquaintances) { profile ->
                    ProfileCard(
                        alias = profile.alias,
                        peerId = profile.peerId,
                        isAcquaintance = true,
                        onAdd = {}
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Nearby Profiles", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (nearby.isEmpty()) {
            Text("No one nearby. Turn on the sharing toggle?", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(nearby) { profile ->
                    ProfileCard(
                        alias = profile.alias,
                        peerId = profile.peerId,
                        isAcquaintance = false,
                        onAdd = { viewModel.addAcquaintance(profile.peerId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCard(alias: String, peerId: String, isAcquaintance: Boolean, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(alias, style = MaterialTheme.typography.titleMedium)
                Text("ID: $peerId", style = MaterialTheme.typography.labelSmall)
            }
            if (!isAcquaintance) {
                Button(onClick = onAdd) {
                    Text("Add")
                }
            } else {
                Text("Acquaintance", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
