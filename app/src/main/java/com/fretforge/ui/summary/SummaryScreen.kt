package com.fretforge.ui.summary

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeSession
import com.fretforge.data.PracticeSessionTask
import com.fretforge.ui.practice.formatTime
import com.fretforge.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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
    val tasks   by viewModel.tasks.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Session Summary",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                },
                actions = {
                    session?.let { sess ->
                        IconButton(onClick = {
                            shareSessionAsImage(context, sess, tasks)
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share session",
                                tint = AmberGold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard)
            )
        },
        bottomBar = {
            Surface(
                color          = DarkCard,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Share button
                    session?.let { sess ->
                        OutlinedButton(
                            onClick = { shareSessionAsImage(context, sess, tasks) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(AmberGold, ElectricBlue))
                            )
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Share Session",
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Done button
                    Button(
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor   = OnDarkPrimary
                        )
                    ) {
                        Text(
                            "Done — Back to Library",
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { padding ->
        session?.let { sess ->
            LazyColumn(
                modifier            = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header hero
                item {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Trophy icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(AmberGoldDark.copy(alpha = 0.5f), AmberGoldDim)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint     = AmberGold,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gradient headline
                        Text(
                            text  = "Great Job!",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                brush      = Brush.horizontalGradient(
                                    colors = listOf(AmberGoldLight, ElectricBlueLight)
                                )
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Total time card
                        Surface(
                            modifier       = Modifier.fillMaxWidth(),
                            color          = DarkCard,
                            shape          = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(AmberGoldDark.copy(alpha = 0.15f), DarkCard)
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "TOTAL PRACTICE TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text  = formatTime(sess.totalDurationSeconds),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight    = FontWeight.ExtraBold,
                                        fontFamily    = FontFamily.Monospace,
                                        color         = AmberGold,
                                        letterSpacing = (-1).sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Section header
                item {
                    Text(
                        "Tasks Completed",
                        style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color    = OnDarkSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Task cards
                items(tasks, key = { it.id }) { task ->
                    Surface(
                        modifier       = Modifier.fillMaxWidth(),
                        color          = DarkCard,
                        shape          = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier          = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.taskName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = OnDarkSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))

                            // Time chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElectricBlueDim
                            ) {
                                Text(
                                    text     = formatTime(task.timeSpentSeconds),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style    = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color      = ElectricBlueLight,
                                    fontWeight = FontWeight.Bold
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style    = MaterialTheme.typography.labelMedium,
                                    color    = AmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AmberGold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Share helper — programmatically draws session card as a Bitmap
// ─────────────────────────────────────────────────────────────────────────────

fun shareSessionAsImage(
    context: Context,
    session: PracticeSession,
    tasks: List<PracticeSessionTask>
) {
    val bitmap = buildShareBitmap(session, tasks)
    val file   = File(context.cacheDir, "share/fretforge_session.png").also {
        it.parentFile?.mkdirs()
    }
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type    = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Check out my practice session! Shared via FretForge.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share via…"))
}

private fun buildShareBitmap(
    session: PracticeSession,
    tasks: List<PracticeSessionTask>
): Bitmap {
    // Dimensions
    val width   = 1080
    val rowH    = 72
    val padding = 60
    val headerH = 320
    val footerH = 100
    val height  = headerH + (tasks.size * rowH) + footerH + padding

    val bmp    = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Colours
    val bgColor        = Color(0xFF0D0D0F).toArgb()
    val cardColor      = Color(0xFF18181C).toArgb()
    val amberColor     = Color(0xFFE8A838).toArgb()
    val amberLight     = Color(0xFFFFC85A).toArgb()
    val blueColor      = Color(0xFF5C8EFF).toArgb()
    val blueLight      = Color(0xFF8AB0FF).toArgb()
    val textPrimary    = Color(0xFFEEEEF2).toArgb()
    val textSecondary  = Color(0xFF9999AA).toArgb()
    val white          = android.graphics.Color.WHITE

    // Background
    canvas.drawColor(bgColor)

    // Gradient header stripe
    val gradPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(amberColor, blueColor),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), 8f, gradPaint)

    // App name
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = white
        textSize  = 84f
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        shader    = LinearGradient(
            0f, 0f, 600f, 0f,
            intArrayOf(amberLight, blueLight),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawText("FretForge", padding.toFloat(), 110f, titlePaint)

    // Date line
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = textSecondary
        textSize = 38f
    }
    val dateStr = SimpleDateFormat("EEE, d MMM yyyy  •  h:mm a", Locale.getDefault())
        .format(Date(session.startTimestamp))
    canvas.drawText(dateStr, padding.toFloat(), 165f, datePaint)

    // "Great job!" line
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = textSecondary
        textSize = 40f
    }
    canvas.drawText("Great job! 🎸", padding.toFloat(), 220f, subPaint)

    // Total time card
    val cardRect = RectF(
        padding.toFloat(), 248f,
        (width - padding).toFloat(), 310f
    )
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardColor }
    canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint)

    val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = textSecondary
        textSize = 28f
        letterSpacing = 0.15f
    }
    canvas.drawText("TOTAL PRACTICE TIME", padding + 20f, 273f, totalLabelPaint)

    val totalTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = amberColor
        textSize = 46f
        typeface = Typeface.MONOSPACE
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    canvas.drawText(formatTime(session.totalDurationSeconds), 600f, 295f, totalTimePaint)

    // Task rows
    var y = headerH.toFloat()
    tasks.forEach { task ->
        val rowRect = RectF(
            padding.toFloat(), y + 6f,
            (width - padding).toFloat(), y + rowH - 6f
        )
        canvas.drawRoundRect(rowRect, 14f, 14f, cardPaint)

        val taskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color    = textPrimary
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(task.taskName, padding + 20f, y + rowH * 0.62f, taskPaint)

        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color    = amberColor
            textSize = 30f
            typeface = Typeface.MONOSPACE
        }
        val timeStr = "${formatTime(task.timeSpentSeconds)}  ${task.bpmUsed} BPM"
        val tw      = chipPaint.measureText(timeStr)
        canvas.drawText(timeStr, width - padding - 20f - tw, y + rowH * 0.62f, chipPaint)

        y += rowH
    }

    // Footer
    val footerY  = y + 30f
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = textSecondary
        textSize = 30f
    }
    val footerText = "Shared via FretForge • Download from Play Store"
    val fw = footerPaint.measureText(footerText)
    canvas.drawText(footerText, (width - fw) / 2f, footerY + 34f, footerPaint)

    // Bottom gradient stripe
    val gradPaint2 = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(blueColor, amberColor),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, height - 8f, width.toFloat(), height.toFloat(), gradPaint2)

    return bmp
}
