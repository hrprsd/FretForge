package com.fretforge.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    navController: NavController,
    viewModel: GroupsViewModel = viewModel()
) {
    val groups by viewModel.groups.collectAsState()
    var groupToDelete by remember { mutableStateOf<PracticeGroup?>(null) }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Delete Group?") },
            text = { Text("Are you sure you want to delete ${groupToDelete?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    groupToDelete?.let { viewModel.deleteGroup(it.groupId) }
                    groupToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Saved Groups") }) }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No saved practice groups.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.groupId }) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            navController.navigate("practice/${group.taskIds}")
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                val taskCount = group.taskIds.split(",").size
                                Text("$taskCount tasks", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                navController.navigate("group_review/${group.taskIds}?groupId=${group.groupId}")
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                groupToDelete = group
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                            IconButton(onClick = {
                                navController.navigate("practice/${group.taskIds}")
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start Practice")
                            }
                        }
                    }
                }
            }
        }
    }
}
