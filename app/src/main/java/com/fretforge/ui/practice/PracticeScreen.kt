package com.fretforge.ui.practice

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun PracticeScreen(
    navController: NavController,
    viewModel: PracticeViewModel = viewModel()
) {
    val navBackStackEntry = navController.currentBackStackEntry
    val taskIdsStr = navBackStackEntry?.arguments?.getString("taskIds") ?: 
                     navBackStackEntry?.arguments?.getString("groupId") ?: ""
                     
    LaunchedEffect(Unit) {
        val rawArg = navController.currentBackStackEntry?.arguments?.getString("groupId") ?: navController.currentBackStackEntry?.arguments?.getString("taskIds")
        viewModel.loadTasks(rawArg ?: "")
    }

    val tasks by viewModel.tasks.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentTask = viewModel.currentTask
    val bpm by viewModel.bpm.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val activeBeat by viewModel.activeBeatIndex.collectAsState()
    val beatStates by viewModel.beatStates.collectAsState()
    val taskTime by viewModel.taskTimeSeconds.collectAsState()
    val totalTime by viewModel.totalSessionTimeSeconds.collectAsState()

    var isImageExpanded by remember { mutableStateOf(false) }

    if (tasks.isEmpty() || currentTask == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP SECTION - 40%
        Box(
            modifier = Modifier.weight(0.4f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Task ${currentIndex + 1} of ${tasks.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentTask.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                currentTask.imageFile?.let { img ->
                    val context = LocalContext.current
                    val bmp = remember(img) {
                        try {
                            context.assets.open("images/$img").use { BitmapFactory.decodeStream(it) }
                        } catch (e: Exception) { null }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Tap to enlarge",
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isImageExpanded = true }
                        )

                        if (isImageExpanded) {
                            Dialog(
                                onDismissRequest = { isImageExpanded = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                        .clickable { isImageExpanded = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Enlarged Image",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BOTTOM SECTION - 60%
        Box(modifier = Modifier.weight(0.6f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Metronome Dots
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    beatStates.forEachIndexed { index, state ->
                        val isActive = activeBeat == index
                        val scale by animateFloatAsState(targetValue = if (isActive) 1.3f else 1.0f)
                        val color = when(state) {
                            1 -> MaterialTheme.colorScheme.primary
                            2 -> Color.DarkGray
                            else -> MaterialTheme.colorScheme.secondary
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { viewModel.toggleBeatState(index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // BPM Controls
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { viewModel.updateBpm(bpm - 1) }) { Text("-1") }
                        Text("♩ = $bpm BPM", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        FilledTonalButton(onClick = { viewModel.updateBpm(bpm + 1) }) { Text("+1") }
                    }
                    
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { viewModel.updateBpm(it.toInt()) },
                        valueRange = 50f..500f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { viewModel.updateBpm(bpm - 5) }) { Text("-5") }
                        
                        FilledTonalButton(
                            onClick = { viewModel.toggleMetronome() },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        FilledTonalButton(onClick = { viewModel.updateBpm(bpm + 5) }) { Text("+5") }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Stopwatches
                Text(text = "This Task", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(
                    text = formatTime(taskTime),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(text = "Total Session: ${formatTime(totalTime)}", style = MaterialTheme.typography.titleMedium)
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.endSession { sessionId ->
                                navController.navigate("summary/$sessionId") {
                                    popUpTo("practice") { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("End")
                    }
                    
                    Button(
                        onClick = { viewModel.nextTask() },
                        modifier = Modifier.weight(1f),
                        enabled = currentIndex < tasks.lastIndex
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
