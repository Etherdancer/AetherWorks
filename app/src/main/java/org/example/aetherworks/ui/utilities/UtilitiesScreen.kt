package org.example.aetherworks.ui.utilities

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    modifier: Modifier = Modifier,
    onNavigateToVault: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToMedia: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Utilities") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Utilities",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "All-In-One Toolkit",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = "Local-first utilities built directly into your private sovereign node.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            UtilityCard(title = "Password & OTP Vault", onClick = onNavigateToVault)
            UtilityCard(title = "Local Calendar", onClick = onNavigateToCalendar)
            UtilityCard(title = "Habits & Tasks", onClick = onNavigateToTasks)
            UtilityCard(title = "Media Player", onClick = onNavigateToMedia)
        }
    }
}

@Composable
private fun UtilityCard(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
