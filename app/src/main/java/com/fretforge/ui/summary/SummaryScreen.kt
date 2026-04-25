package com.fretforge.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.ui.practice.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    viewModel: SummaryViewModel = viewModel()
) {
    val navBackStackEntry = navController.currentBackStackEntry
    val sessionId = navBackStackEntry?.arguments?.getLong("sessionId") ?: 0L

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val session by viewModel.session.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Session Summary") }) },
        bottomBar = {
            Button(
                onClick = { 
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Done")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            session?.let {
                Text("Great job!", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Practice Time: ${formatTime(it.totalDurationSeconds)}")
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Tasks Completed:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(task.taskName, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Time: ${formatTime(task.timeSpentSeconds)}")
                                    Text("BPM: ${task.bpmUsed}")
                                }
                            }
                        }
                    }
                }
            } ?: run {
                CircularProgressIndicator()
            }
        }
    }
}
