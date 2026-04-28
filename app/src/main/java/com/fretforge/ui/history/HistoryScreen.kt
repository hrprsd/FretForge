package com.fretforge.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeSession
import com.fretforge.data.PracticeSessionTask
import com.fretforge.ui.components.LocalDrawerState
import com.fretforge.ui.practice.formatTime
import com.fretforge.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = viewModel()
) {
    val sessions        by viewModel.sessions.collectAsState()
    val sessionTasksMap by viewModel.sessionTasks.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<PracticeSession?>(null) }
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor   = DarkCard,
            title = {
                Text(
                    "Delete Session?",
                    color = OnDarkSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this practice session from your history?",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog?.let { viewModel.deleteSession(it.sessionId) }
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
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
                        "History",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = OnDarkSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard)
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint     = TextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No practice sessions yet.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Start practicing to see your history here.",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    val tasks = sessionTasksMap[session.sessionId] ?: emptyList()
                    HistorySessionCard(
                        session  = session,
                        tasks    = tasks,
                        onDelete = { showDeleteDialog = session }
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySessionCard(
    session: PracticeSession,
    tasks: List<PracticeSessionTask>,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        color  = DarkCard,
        shape  = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column {
            // Top gradient stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ElectricBlue, AmberGold)
                        )
                    )
            )

            // Header row
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val dateStr = SimpleDateFormat(
                        "EEE, d MMM yyyy",
                        Locale.getDefault()
                    ).format(Date(session.startTimestamp))
                    val timeStr = SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                    ).format(Date(session.startTimestamp))

                    Text(
                        dateStr,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                    Text(
                        timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Duration chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AmberGoldDim
                    ) {
                        Text(
                            text     = "⏱ ${formatTime(session.totalDurationSeconds)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color    = AmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed.copy(alpha = 0.75f)
                    )
                }

                Icon(
                    imageVector  = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint         = TextSecondary,
                    modifier     = Modifier.padding(end = 8.dp)
                )
            }

            // Expandable task list
            AnimatedVisibility(
                visible    = expanded,
                enter      = expandVertically(),
                exit       = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .background(DarkCardElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(color = DarkOutline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(2.dp))

                    tasks.forEachIndexed { idx, task ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = task.taskName,
                                modifier = Modifier.weight(1f),
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = OnDarkSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Time chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElectricBlueDim
                            ) {
                                Text(
                                    text     = formatTime(task.timeSpentSeconds),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style    = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color    = ElectricBlueLight
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            // BPM chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberGoldDim
                            ) {
                                Text(
                                    text     = "${task.bpmUsed} BPM",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = AmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
