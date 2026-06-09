package app.clearspace.network.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(modifier: Modifier = Modifier, onBack: () -> Unit, viewModel: GroupsViewModel = viewModel()) {
    val groups by viewModel.groups.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var groupNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trusted Groups") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Group")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    Text("No groups created. Create one to share content with a specific list of trusted users.")
                }
            } else {
                items(groups) { groupWithMembers ->
                    var showDetailDialog by remember { mutableStateOf(false) }
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                        showDetailDialog = true
                    }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(groupWithMembers.group.name, style = MaterialTheme.typography.titleMedium)
                            Text("${groupWithMembers.members.size} members", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (showDetailDialog) {
                        GroupDetailDialog(
                            groupName = groupWithMembers.group.name,
                            members = groupWithMembers.members.map { it.publicKeyBase64 },
                            onDismiss = { showDetailDialog = false },
                            onAddMember = { viewModel.addMemberToGroup(groupWithMembers.group.groupId, it) },
                            onRemoveMember = { viewModel.removeMemberFromGroup(groupWithMembers.group.groupId, it) }
                        )
                    }
                }
            }
        }
        
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Group") },
                text = {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (groupNameInput.isNotBlank()) {
                            viewModel.createGroup(groupNameInput)
                            groupNameInput = ""
                            showCreateDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GroupDetailDialog(
    groupName: String,
    members: List<String>,
    onDismiss: () -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    var newMemberAlias by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(groupName) },
        text = {
            Column {
                Text("Members:", style = MaterialTheme.typography.labelLarge)
                if (members.isEmpty()) {
                    Text("No members yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(members) { member ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(member.take(16) + "...")
                                IconButton(onClick = { onRemoveMember(member) }) {
                                    Text("X", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newMemberAlias,
                    onValueChange = { newMemberAlias = it },
                    label = { Text("Add Member Public Key") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newMemberAlias.isNotBlank()) {
                    onAddMember(newMemberAlias)
                    newMemberAlias = ""
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
