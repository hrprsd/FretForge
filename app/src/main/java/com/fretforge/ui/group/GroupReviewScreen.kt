package com.fretforge.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeTask
import com.fretforge.ui.theme.*

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

    val tasks     by viewModel.tasks.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor   = DarkCard,
            title = {
                Text(
                    "Discard changes?",
                    color = OnDarkSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    "Going back will discard your current practice group setup.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (groupId > 0) "Edit Group" else "Group Review",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OnDarkSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard)
            )
        },
        bottomBar = {
            Surface(
                color = DarkCard,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveGroup(groupId) {
                                navController.navigate("groups") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        enabled = tasks.isNotEmpty() && groupName.isNotBlank(),
                        shape   = RoundedCornerShape(14.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor   = OnDarkSecondary,
                            disabledContainerColor = DarkOutline
                        )
                    ) {
                        Text(
                            "Save Group",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = {
                            val finalIds = tasks.joinToString(",") { it.id.toString() }
                            navController.navigate("practice/$finalIds")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        enabled = tasks.isNotEmpty(),
                        shape   = RoundedCornerShape(14.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor   = OnDarkPrimary,
                            disabledContainerColor = DarkOutline
                        )
                    ) {
                        Text(
                            "Start Practice",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Group name field
            OutlinedTextField(
                value         = groupName,
                onValueChange = { viewModel.updateGroupName(it) },
                label         = { Text("Group Name", color = TextSecondary) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                singleLine    = true,
                shape         = RoundedCornerShape(14.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = AmberGold,
                    unfocusedBorderColor  = DarkOutline,
                    focusedContainerColor   = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    cursorColor           = AmberGold,
                    focusedTextColor      = OnDarkSurface,
                    unfocusedTextColor    = OnDarkSurface,
                    focusedLabelColor     = AmberGold
                )
            )

            Text(
                "Reorder tasks",
                style    = MaterialTheme.typography.labelSmall,
                color    = TextTertiary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    TaskReorderRow(
                        task      = task,
                        isFirst   = index == 0,
                        isLast    = index == tasks.lastIndex,
                        onMoveUp   = { viewModel.moveTask(index, index - 1) },
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
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        color          = DarkCard,
        shape          = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier          = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numbered index pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberGoldDim
            ) {
                Text(
                    text     = "${(task.id % 100).toInt()}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = AmberGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.name,
                    style  = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color  = OnDarkSurface
                )
                if (task.category.isNotEmpty()) {
                    Text(
                        task.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Column {
                IconButton(
                    onClick  = onMoveUp,
                    enabled  = !isFirst,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = if (!isFirst) AmberGold else TextTertiary
                    )
                }
                IconButton(
                    onClick  = onMoveDown,
                    enabled  = !isLast,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = if (!isLast) AmberGold else TextTertiary
                    )
                }
            }
        }
    }
}
