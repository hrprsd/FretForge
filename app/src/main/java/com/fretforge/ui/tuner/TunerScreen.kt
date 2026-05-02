package com.fretforge.ui.tuner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hrprsd.fretforge.R
import com.fretforge.ui.components.LocalDrawerState
import com.fretforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(viewModel: TunerViewModel = viewModel()) {
    val context     = LocalContext.current
    val drawerState = LocalDrawerState.current
    val scope       = rememberCoroutineScope()
    val state       by viewModel.state.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startListening() else viewModel.stopListening()
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopListening() } }

    Scaffold(
        containerColor = Color(0xFF111118),
        topBar = {
            TopAppBar(
                title = {
                    Text("Tune Guitar",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface)
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, "Menu", tint = OnDarkSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111118))
            )
        }
    ) { padding ->
        if (!hasPermission) {
            PermissionRequest(
                modifier = Modifier.fillMaxSize().padding(padding),
                onRequest = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Top: Tuning meter ─────────────────────────────────────
            CircularTunerMeter(
                modifier     = Modifier.fillMaxWidth().weight(0.38f),
                state        = state
            )
            // ── Bottom: Headstock + string buttons ────────────────────
            HeadstockPanel(
                modifier          = Modifier.fillMaxWidth().weight(0.62f),
                activeIndex       = state.activeString?.pegIndex,
                tuneStatus        = state.status
            )
        }
    }
}

// ── Circular Tuning Meter ────────────────────────────────────────────────────
@Composable
private fun CircularTunerMeter(modifier: Modifier, state: TunerState) {
    val centsTarget = if (state.activeString != null) state.centsOff.coerceIn(-50f, 50f) else 0f
    val animatedCents by animateFloatAsState(
        targetValue   = centsTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label         = "tuningDial"
    )
    val indicatorColor by animateColorAsState(
        targetValue = when (state.status) {
            TuneStatus.IN_TUNE   -> Color(0xFF4CAF82)
            TuneStatus.SHARP     -> ErrorRed
            TuneStatus.FLAT      -> ElectricBlue
            TuneStatus.LISTENING -> Color.White.copy(alpha = 0.3f)
        },
        label = "indicatorColor"
    )

    Box(modifier = modifier.background(Color(0xFF0D0D14)), contentAlignment = Alignment.Center) {
        // Subtle dot-grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridPx = 28.dp.toPx()
            var x = 0f
            while (x <= size.width)  { drawLine(Color.White.copy(0.05f), Offset(x, 0f), Offset(x, size.height), 1f); x += gridPx }
            var y = 0f
            while (y <= size.height) { drawLine(Color.White.copy(0.05f), Offset(0f, y), Offset(size.width, y), 1f); y += gridPx }
        }

        // Circular Dial Canvas
        Canvas(modifier = Modifier.size(260.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 20.dp.toPx()
            
            // Background Arc (semi-circle)
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )

            // Tick marks
            val angles = listOf(135f, 135f + 67.5f, 270f, 270f + 67.5f, 405f)
            angles.forEach { angle ->
                val angleRad = angle * (PI / 180f)
                val innerRadius = radius - 16.dp.toPx()
                val outerRadius = radius + 10.dp.toPx()
                val start = Offset(
                    center.x + (innerRadius * kotlin.math.cos(angleRad)).toFloat(),
                    center.y + (innerRadius * kotlin.math.sin(angleRad)).toFloat()
                )
                val end = Offset(
                    center.x + (outerRadius * kotlin.math.cos(angleRad)).toFloat(),
                    center.y + (outerRadius * kotlin.math.sin(angleRad)).toFloat()
                )
                val isCenter = angle == 270f
                drawLine(
                    color = if (isCenter) Color.White.copy(0.5f) else Color.White.copy(0.2f),
                    start = start,
                    end = end,
                    strokeWidth = if (isCenter) 4.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Indicator Arc
            if (state.activeString != null) {
                // fraction from -1 to 1 mapped to angles 135 to 405
                // 0 cents = 270 degrees
                val fraction = animatedCents / 50f
                val sweepAngle = fraction * 135f
                
                // Draw colored arc from center to indicator
                val startA = 270f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(ElectricBlue, Color(0xFF4CAF82), ErrorRed),
                        center = center
                    ),
                    startAngle = startA,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                    alpha = 0.8f
                )

                // Draw needle dot
                val needleAngleRad = (270f + sweepAngle) * (PI / 180f)
                val needlePos = Offset(
                    center.x + (radius * kotlin.math.cos(needleAngleRad)).toFloat(),
                    center.y + (radius * kotlin.math.sin(needleAngleRad)).toFloat()
                )
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = needlePos
                )
                drawCircle(
                    color = indicatorColor,
                    radius = 8.dp.toPx(),
                    center = needlePos
                )
            }
        }

        // ♭ / ♯ labels
        Box(modifier = Modifier.size(260.dp)) {
            Text("♭", modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 24.dp),
                color = ElectricBlue.copy(alpha = 0.7f),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("♯", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp),
                color = ErrorRed.copy(alpha = 0.7f),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }

        // Center Note Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.activeString != null) {
                // Display just the letter (e.g., 'E' instead of 'E2')
                val noteLetter = state.activeString.label.first().toString()
                
                Text(
                    text = noteLetter,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 100.sp,
                        color = indicatorColor
                    ),
                    modifier = Modifier.shadow(
                        elevation = if (state.status == TuneStatus.IN_TUNE) 24.dp else 0.dp,
                        shape = CircleShape,
                        ambientColor = indicatorColor,
                        spotColor = indicatorColor
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val statusText = when (state.status) {
                    TuneStatus.IN_TUNE   -> "✓ In Tune"
                    TuneStatus.SHARP     -> "Tune Down (${ "%.0f".format(state.centsOff)}¢)"
                    TuneStatus.FLAT      -> "Tune Up (${ "%.0f".format(state.centsOff)}¢)"
                    TuneStatus.LISTENING -> "${ "%.1f".format(state.detectedHz)} Hz"
                }
                Text(statusText, color = indicatorColor.copy(0.9f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 100.sp,
                        color = Color.White.copy(0.1f)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Play a string to tune", color = Color.White.copy(0.3f),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ── Headstock panel ───────────────────────────────────────────────────────────
@Composable
private fun HeadstockPanel(
    modifier: Modifier,
    activeIndex: Int?,
    tuneStatus: TuneStatus
) {
    // Left peg layout (top→bottom): D(3), A(4), E(5)
    val leftStrings  = listOf(
        Triple("D", 3, TunerViewModel.STRINGS[2].targetHz),
        Triple("A", 4, TunerViewModel.STRINGS[1].targetHz),
        Triple("E", 5, TunerViewModel.STRINGS[0].targetHz)
    )
    // Right peg layout (top→bottom): G(2), B(1), E(0)
    val rightStrings = listOf(
        Triple("G", 2, TunerViewModel.STRINGS[3].targetHz),
        Triple("B", 1, TunerViewModel.STRINGS[4].targetHz),
        Triple("E", 0, TunerViewModel.STRINGS[5].targetHz)
    )

    BoxWithConstraints(
        // Transparent — blends with the page background
        modifier = modifier.background(Color.Transparent)
    ) {
        val totalH = maxHeight

        // Headstock image — enlarged, blends via graphicsLayer
        androidx.compose.foundation.Image(
            painter            = painterResource(R.drawable.guitar_headstock),
            contentDescription = "Guitar headstock",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .width(maxWidth * 0.72f)   // ← wider than before
                .fillMaxHeight()
                .align(Alignment.Center)
                .graphicsLayer { alpha = 0.92f }
        )

        // Left column
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .fillMaxHeight()
                .padding(top = totalH * 0.05f, bottom = totalH * 0.08f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            leftStrings.forEach { (label, pegIdx, hz) ->
                PegButton(
                    label       = label,
                    pegIndex    = pegIdx,
                    activeIndex = activeIndex,
                    status      = tuneStatus,
                    size        = 56.dp,
                    stringHz    = hz
                )
            }
        }

        // Right column
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .fillMaxHeight()
                .padding(top = totalH * 0.05f, bottom = totalH * 0.08f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rightStrings.forEach { (label, pegIdx, hz) ->
                PegButton(
                    label       = label,
                    pegIndex    = pegIdx,
                    activeIndex = activeIndex,
                    status      = tuneStatus,
                    size        = 56.dp,
                    stringHz    = hz
                )
            }
        }
    }
}

// ── String peg button ─────────────────────────────────────────────────────────
@Composable
private fun PegButton(
    label: String,
    pegIndex: Int,
    activeIndex: Int?,
    status: TuneStatus,
    size: Dp,
    stringHz: Float        // target frequency for audio preview
) {
    val scope    = rememberCoroutineScope()
    val isActive = pegIndex == activeIndex
    val glowColor by animateColorAsState(
        targetValue = if (!isActive) Color.Transparent else when (status) {
            TuneStatus.IN_TUNE   -> Color(0xFF4CAF82)
            TuneStatus.SHARP     -> ErrorRed
            TuneStatus.FLAT      -> ElectricBlue
            TuneStatus.LISTENING -> AmberGold
        },
        label = "glowColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) glowColor.copy(alpha = 0.20f) else Color(0xFF1E1E2A),
        label = "bgColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) glowColor else Color.White.copy(0.7f),
        label = "textColor"
    )

    // Pulse scale when active & in tune
    val scale by if (isActive && status == TuneStatus.IN_TUNE) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue  = 1f,
            targetValue   = 1.08f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label         = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Box(
        modifier = Modifier
            .size(size * scale)
            .shadow(if (isActive) 12.dp else 0.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { scope.launch(Dispatchers.IO) { playStringTone(stringHz) } },
        contentAlignment = Alignment.Center
    ) {
        // Ring border
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isActive) {
                drawCircle(glowColor.copy(0.6f), radius = size.toPx() / 2 * scale - 2f, style = Stroke(2.5f))
            } else {
                drawCircle(Color.White.copy(0.12f), radius = size.toPx() / 2 - 2f, style = Stroke(1f))
            }
        }
        Text(
            text  = label,
            color = textColor,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize   = 18.sp
            )
        )
    }
}

// ── Sine-wave tone player ─────────────────────────────────────────────────────
private fun playStringTone(hz: Float, durationMs: Int = 1200) {
    val sampleRate  = 44100
    val numSamples  = sampleRate * durationMs / 1000
    val samples     = ShortArray(numSamples)
    val twoPiF      = 2.0 * PI * hz / sampleRate

    // Generate sine + gentle amplitude envelope (attack + decay)
    for (i in 0 until numSamples) {
        val env = when {
            i < sampleRate * 0.01  -> i / (sampleRate * 0.01)          // 10 ms attack
            i > numSamples * 0.5   -> (numSamples - i).toDouble() / (numSamples * 0.5) // decay
            else                   -> 1.0
        }
        samples[i] = (sin(twoPiF * i) * env * Short.MAX_VALUE * 0.7).toInt().toShort()
    }

    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(samples.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    track.write(samples, 0, samples.size)
    track.play()
    // Block until done then release
    Thread.sleep(durationMs.toLong())
    track.stop()
    track.release()
}

// ── Permission request ────────────────────────────────────────────────────────
@Composable
private fun PermissionRequest(modifier: Modifier, onRequest: () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("🎤", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Microphone Access Needed",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = OnDarkSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("FretForge needs your microphone to detect guitar string pitches in real-time.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest, shape = RoundedCornerShape(14.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = OnDarkPrimary)
        ) { Text("Grant Permission", fontWeight = FontWeight.Bold) }
    }
}
