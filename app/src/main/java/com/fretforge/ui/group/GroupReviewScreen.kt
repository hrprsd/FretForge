package com.fretforge.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReviewScreen(
    navController: NavController,
    taskIds: String,
    groupId: Long = 0L,
    viewModel: GroupReviewViewModel = viewModel()
) {
    LaunchedEffect(taskIds, groupId) {
        viewModel.loadTasks(taskIds, groupId)
    }

    val tasks by viewModel.tasks.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Going back will discard your current practice group setup.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupId > 0) "Edit Group" else "Group Review") },
                navigationIcon = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveGroup(groupId) {
                            navController.navigate("groups") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = tasks.isNotEmpty() && groupName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Save Group", modifier = Modifier.padding(8.dp))
                }
                
                Button(
                    onClick = {
                        val finalIds = tasks.joinToString(",") { it.id.toString() }
                        navController.navigate("practice/$finalIds")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = tasks.isNotEmpty()
                ) {
                    Text("Start Practice", modifier = Modifier.padding(8.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { viewModel.updateGroupName(it) },
                label = { Text("Group Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    TaskReorderRow(
                        task = task,
                        isFirst = index == 0,
                        isLast = index == tasks.lastIndex,
                        onMoveUp = { viewModel.moveTask(index, index - 1) },
                        onMoveDown = { viewModel.moveTask(index, index + 1) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskReorderRow(
    task: PracticeTask,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(task.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                }
            }
        }
    }
}
