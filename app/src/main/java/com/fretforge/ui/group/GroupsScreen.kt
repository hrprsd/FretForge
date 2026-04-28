package com.fretforge.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeGroup
import com.fretforge.ui.components.LocalDrawerState
import com.fretforge.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    navController: NavController,
    viewModel: GroupsViewModel = viewModel()
) {
    val groups by viewModel.groups.collectAsState()
    var groupToDelete by remember { mutableStateOf<PracticeGroup?>(null) }
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            containerColor   = DarkCard,
            title = {
                Text(
                    "Delete Group?",
                    color = OnDarkSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${groupToDelete?.name}\"?",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupToDelete?.let { viewModel.deleteGroup(it.groupId) }
                        groupToDelete = null
                    }
                ) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
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
                        "Groups",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = OnDarkSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCard
                )
            )
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint   = TextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No saved practice groups yet.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Create one from the Home tab.",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding       = PaddingValues(16.dp),
                verticalArrangement  = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.groupId }) { group ->
                    GroupCard(
                        group        = group,
                        onEdit       = { navController.navigate("group_review/${group.taskIds}?groupId=${group.groupId}") },
                        onDelete     = { groupToDelete = group },
                        onStartPractice = { navController.navigate("practice/${group.taskIds}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: PracticeGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartPractice: () -> Unit
) {
    val taskCount = group.taskIds.split(",").size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color  = DarkCard,
        shape  = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        // Gradient top-stripe accent
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AmberGold, ElectricBlue)
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = group.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ElectricBlueDim
                    ) {
                        Text(
                            text     = "$taskCount ${if (taskCount == 1) "task" else "tasks"}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = ElectricBlueLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action icons
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed.copy(alpha = 0.8f)
                    )
                }

                // Pill play button
                FilledTonalButton(
                    onClick = onStartPractice,
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AmberGold,
                        contentColor   = OnDarkPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start Practice",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
