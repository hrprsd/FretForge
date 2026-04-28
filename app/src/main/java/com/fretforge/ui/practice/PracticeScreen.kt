package com.fretforge.ui.practice

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.ui.theme.*

@Composable
fun PracticeScreen(
    navController: NavController,
    viewModel: PracticeViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        val rawArg = navController.currentBackStackEntry?.arguments?.getString("groupId")
            ?: navController.currentBackStackEntry?.arguments?.getString("taskIds")
        viewModel.loadTasks(rawArg ?: "")
    }

    val tasks        by viewModel.tasks.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentTask  = viewModel.currentTask
    val bpm          by viewModel.bpm.collectAsState()
    val isPlaying    by viewModel.isPlaying.collectAsState()
    val activeBeat   by viewModel.activeBeatIndex.collectAsState()
    val beatStates   by viewModel.beatStates.collectAsState()
    val taskTime     by viewModel.taskTimeSeconds.collectAsState()
    val totalTime    by viewModel.totalSessionTimeSeconds.collectAsState()

    var isImageExpanded by remember { mutableStateOf(false) }

    if (tasks.isEmpty() || currentTask == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AmberGold)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── TOP SECTION — 40% ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // Progress indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AmberGoldDim
                    ) {
                        Text(
                            text     = "Task ${currentIndex + 1} of ${tasks.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = AmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text  = currentTask.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = OnDarkSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Task image
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
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
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
                                        .background(Color.Black.copy(alpha = 0.95f))
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCard)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── BOTTOM SECTION — 60% ──────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Beat Dots ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    beatStates.forEachIndexed { index, state ->
                        val isActive = activeBeat == index
                        val scale by animateFloatAsState(
                            targetValue = if (isActive) 1.3f else 1.0f,
                            animationSpec = tween(durationMillis = 80),
                            label = "beat_scale"
                        )
                        val dotColor = when (state) {
                            1 -> BeatActive
                            2 -> BeatMuted
                            else -> BeatAccent
                        }
                        val borderColor = if (isActive) dotColor else dotColor.copy(alpha = 0.4f)

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) dotColor else dotColor.copy(alpha = 0.25f)
                                )
                                .border(2.dp, borderColor, CircleShape)
                                .clickable { viewModel.toggleBeatState(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── BPM Display ────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color    = DarkCard,
                    shape    = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // BPM value row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateBpm(bpm - 1) },
                                shape   = RoundedCornerShape(10.dp),
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                border  = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(AmberGold, AmberGold))),
                                modifier = Modifier.size(width = 52.dp, height = 40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("−1", fontWeight = FontWeight.Bold) }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text  = "$bpm",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight   = FontWeight.ExtraBold,
                                        color        = AmberGold,
                                        letterSpacing= (-1).sp
                                    )
                                )
                                Text(
                                    "BPM",
                                    style  = MaterialTheme.typography.labelSmall,
                                    color  = TextSecondary,
                                    letterSpacing = 2.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.updateBpm(bpm + 1) },
                                shape   = RoundedCornerShape(10.dp),
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                border  = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(AmberGold, AmberGold))),
                                modifier = Modifier.size(width = 52.dp, height = 40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+1", fontWeight = FontWeight.Bold) }
                        }

                        // Slider
                        Slider(
                            value         = bpm.toFloat(),
                            onValueChange = { viewModel.updateBpm(it.toInt()) },
                            valueRange    = 50f..500f,
                            modifier      = Modifier.fillMaxWidth(),
                            colors        = SliderDefaults.colors(
                                thumbColor            = AmberGold,
                                activeTrackColor      = AmberGold,
                                inactiveTrackColor    = DarkOutline
                            )
                        )

                        // ±5 and Play row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateBpm(bpm - 5) },
                                shape   = RoundedCornerShape(10.dp),
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                border  = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(AmberGold, AmberGold))),
                                modifier = Modifier.size(width = 62.dp, height = 40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("−5", fontWeight = FontWeight.Bold) }

                            // Circular Play/Stop button
                            val playScale by animateFloatAsState(
                                targetValue   = if (isPlaying) 1.08f else 1.0f,
                                animationSpec = tween(120),
                                label         = "play_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .scale(playScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying)
                                            Brush.radialGradient(listOf(ElectricBlue, ElectricBlueDark))
                                        else
                                            Brush.radialGradient(listOf(AmberGold, AmberGoldDark))
                                    )
                                    .clickable { viewModel.toggleMetronome() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector  = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Play",
                                    tint         = if (isPlaying) OnDarkSecondary else OnDarkPrimary,
                                    modifier     = Modifier.size(30.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.updateBpm(bpm + 5) },
                                shape   = RoundedCornerShape(10.dp),
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                border  = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(AmberGold, AmberGold))),
                                modifier = Modifier.size(width = 62.dp, height = 40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+5", fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // ── Stopwatches ────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color    = DarkCard,
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text  = "THIS TASK",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text  = formatTime(taskTime),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight   = FontWeight.ExtraBold,
                                fontFamily   = FontFamily.Monospace,
                                color        = OnDarkSurface,
                                letterSpacing= (-1).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ElectricBlueDim
                        ) {
                            Text(
                                text     = "Session  ${formatTime(totalTime)}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style    = MaterialTheme.typography.labelMedium,
                                color    = ElectricBlueLight,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── End / Next buttons ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.endSession { sessionId ->
                                navController.navigate("summary/$sessionId") {
                                    popUpTo("practice") { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.15f),
                            contentColor   = ErrorRed
                        )
                    ) {
                        Text("End Session", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick  = { viewModel.nextTask() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled  = currentIndex < tasks.lastIndex,
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor   = OnDarkPrimary,
                            disabledContainerColor = DarkOutline,
                            disabledContentColor   = TextTertiary
                        )
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.NavigateNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
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
