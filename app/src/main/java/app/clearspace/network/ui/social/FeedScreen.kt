package app.clearspace.network.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.clearspace.network.storage.db.entity.ContentUnit

/**
 * Mastodon-Style Microblogging UI
 * Displays a chronological feed of short "Notes" (Toots) broadcast over the P2P indexer.
 * Includes support for Content Warnings (CWs) and hashtags.
 */
@Composable
fun FeedScreen() {
    // Simulated feed data for now
    val feedItems = remember { mutableStateListOf<ContentUnit>() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Open Compose Toot Dialog */ }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "P2P Social Feed",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (feedItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No posts nearby. Turn on Sharing to discover peers.")
                }
            } else {
                LazyColumn {
                    items(feedItems.size) { index ->
                        val item = feedItems[index]
                        FeedPostCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(post: ContentUnit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = post.authorAlias, style = MaterialTheme.typography.titleMedium)
                val reputationScore = post.likeTokens.size - post.dislikeTokens.size
                Text(text = "Rep: $reputationScore", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content Warning simulation
            var cwExpanded by remember { mutableStateOf(false) }
            
            if (cwExpanded) {
                Text(text = post.body, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { cwExpanded = false }) {
                    Text("Hide Content")
                }
            } else {
                Text(text = "CW: Sensitive Content", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { cwExpanded = true }) {
                    Text("Show More")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = { /* Like */ }) { Text("👍") }
                Button(onClick = { /* Dislike */ }) { Text("👎") }
                Button(onClick = { /* Reply */ }) { Text("💬") }
            }
        }
    }
}
