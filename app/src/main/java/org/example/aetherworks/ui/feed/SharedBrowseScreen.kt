package org.example.aetherworks.ui.feed

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.example.aetherworks.IContentRenderer
import org.example.aetherworks.security.guard.ContentRendererService
import androidx.compose.ui.unit.dp
import org.example.aetherworks.discovery.ContentHeader
import org.example.aetherworks.storage.db.entity.ContentUnit
import java.text.SimpleDateFormat
import java.util.*
import org.example.aetherworks.ui.feed.SharedBrowseViewModel.SourceFilter
import org.example.aetherworks.ui.components.FlagConstants
import org.example.aetherworks.ui.components.FlagConstants.FlagType
import org.example.aetherworks.reputation.ReputationAgent
import org.example.aetherworks.crypto.KeyManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SharedBrowseScreen(
    modifier: Modifier = Modifier, 
    viewModel: SharedBrowseViewModel,
    onShareToGroup: (String, String) -> Unit = { _, _ -> }
) {
    val headers by viewModel.sharedHeaders.collectAsState()
    val loading by viewModel.loadingHeader.collectAsState()
    val viewingContent by viewModel.viewingContent.collectAsState()

    if (viewingContent != null) {
        ContentDetailOverlay(
            content = viewingContent!!,
            onClose = { viewModel.clearViewingContent() },
            onSave = {
                viewModel.saveToPrivateLibrary(viewingContent!!)
            },
            onVote = { isUpvote ->
                viewModel.vote(viewingContent!!, isUpvote)
            },
            onFlagVote = { flag, category ->
                viewModel.voteFlag(viewingContent!!, flag, category)
            },
            onShareToGroup = onShareToGroup,
            onDeepLinkContent = { hash ->
                viewModel.openContent("local:0", hash)
            }
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = { TopAppBar(title = { Text("Nearby Shared Content") }) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val searchQuery by viewModel.searchQuery.collectAsState()
                val filteredHeaders = headers.filter { 
                    it.second.title.contains(searchQuery, ignoreCase = true) ||
                    it.second.authorAlias.contains(searchQuery, ignoreCase = true) ||
                    it.second.categoryFlags.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Smart Scan (Filter local headers)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        
                        val currentFilter by viewModel.sourceFilter.collectAsState()
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            SegmentedButton(
                                selected = currentFilter == SourceFilter.ALL,
                                onClick = { viewModel.updateSourceFilter(SourceFilter.ALL) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) { Text("All") }
                            SegmentedButton(
                                selected = currentFilter == SourceFilter.ACQUAINTANCES,
                                onClick = { viewModel.updateSourceFilter(SourceFilter.ACQUAINTANCES) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) { Text("Acquaintances") }
                            SegmentedButton(
                                selected = currentFilter == SourceFilter.TRUSTED,
                                onClick = { viewModel.updateSourceFilter(SourceFilter.TRUSTED) },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) { Text("Trusted") }
                        }
                    }

                    if (filteredHeaders.isEmpty()) {
                        item {
                            Text(
                                "No shared content matches your search, or sharing is not enabled on nearby devices.",
                                modifier = Modifier.padding(bottom = 16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        items(filteredHeaders) { (ipPort, header) ->
                            HeaderCard(ipPort = ipPort, header = header) {
                                viewModel.openContent(ipPort, header.contentHash)
                            }
                        }
                    }
                }

                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun HeaderCard(ipPort: String, header: ContentHeader, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(header.title, style = MaterialTheme.typography.titleMedium)
            Text("By ${header.authorAlias} • From $ipPort", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            Text("Published: ${sdf.format(Date(header.timestamp))}", style = MaterialTheme.typography.bodySmall)
            Text("Reputation: ${header.reputationScore}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailOverlay(
    content: ContentUnit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onVote: (Boolean) -> Unit,
    onFlagVote: (FlagType, String) -> Unit,
    onShareToGroup: (String, String) -> Unit,
    onDeepLinkContent: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content.title) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Download, contentDescription = "Save to Private Library")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("By ${content.authorAlias}", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            val context = LocalContext.current
            var htmlContent by remember { mutableStateOf<String?>(null) }
            
            DisposableEffect(content.body) {
                var binder: IContentRenderer? = null
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        binder = IContentRenderer.Stub.asInterface(service)
                        try {
                            htmlContent = binder?.renderMarkdownToHtml(content.body) ?: "Renderer error"
                        } catch (e: Exception) {
                            htmlContent = "Error: ${e.message}"
                        }
                    }
                    override fun onServiceDisconnected(name: ComponentName?) {
                        binder = null
                    }
                }
                
                val intent = Intent(context, ContentRendererService::class.java)
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                
                onDispose {
                    context.unbindService(connection)
                }
            }
            
            if (htmlContent == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false // Critical: No JS allowed
                            settings.domStorageEnabled = false
                            
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString()
                                    if (url != null && url.startsWith("aether://content/")) {
                                        val hash = url.substringAfter("aether://content/")
                                        onDeepLinkContent(hash)
                                        return true
                                    }
                                    return super.shouldOverrideUrlLoading(view, request)
                                }
                            }
                            
                            loadDataWithBaseURL(null, htmlContent!!, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                )
            }
            
            if (content.videoPath != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Video content available at: ${content.videoPath}", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { onVote(true) }) {
                    Text("👍 Like (${content.likeTokens.size})")
                }
                Button(onClick = { onVote(false) }) {
                    Text("👎 Dislike (${content.dislikeTokens.size})")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onShareToGroup(content.title, "[Link to ${content.title}](aether://content/${content.contentHash})") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share to Group")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            FlagVotingSection(content = content, onFlagVote = onFlagVote)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FlagVotingSection(
    content: ContentUnit,
    onFlagVote: (FlagType, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val repAgent = remember { ReputationAgent(context, KeyManager(context)) }

    val totalCategoryVotes = content.categoryTokens.values.sumOf { it.size }
    val totalEmotionVotes = content.emotionTokens.values.sumOf { it.size }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tags & Emotions: $totalCategoryVotes Categories, $totalEmotionVotes Emotions",
                    style = MaterialTheme.typography.titleSmall
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("What is this content about?", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlagConstants.CATEGORIES.forEach { category ->
                        val tokenSet = content.categoryTokens[category] ?: emptySet()
                        val myToken = remember(content.contentHash, category) { repAgent.generateFlagToken(content.contentHash, category) }
                        val iVoted = tokenSet.contains(myToken)
                        
                        FilterChip(
                            selected = iVoted,
                            onClick = { if (!iVoted) onFlagVote(FlagType.CATEGORY, category) },
                            label = { Text("$category (${tokenSet.size})") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("How did this make you feel?", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlagConstants.EMOTIONS.forEach { emotion ->
                        val tokenSet = content.emotionTokens[emotion] ?: emptySet()
                        val myToken = remember(content.contentHash, emotion) { repAgent.generateFlagToken(content.contentHash, emotion) }
                        val iVoted = tokenSet.contains(myToken)
                        
                        FilterChip(
                            selected = iVoted,
                            onClick = { if (!iVoted) onFlagVote(FlagType.EMOTION, emotion) },
                            label = { Text("$emotion (${tokenSet.size})") }
                        )
                    }
                }
            }
        }
    }
}
