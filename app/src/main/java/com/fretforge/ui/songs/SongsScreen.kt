package com.fretforge.ui.songs

import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fretforge.data.Song
import com.fretforge.ui.components.LocalDrawerState
import com.fretforge.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(viewModel: SongsViewModel = viewModel()) {
    val songs       by viewModel.songs.collectAsState()
    val context     = LocalContext.current
    val drawerState = LocalDrawerState.current
    val scope       = rememberCoroutineScope()

    var showAddDialog    by remember { mutableStateOf(false) }
    var songToDelete     by remember { mutableStateOf<Song?>(null) }
    var songForImages    by remember { mutableStateOf<Song?>(null) }
    var songForYouTube   by remember { mutableStateOf<Song?>(null) }

    // Delete confirm dialog
    songToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            containerColor   = DarkCard,
            title = { Text("Delete Song?", color = OnDarkSurface) },
            text  = { Text("Remove \"${song.name}\" from your practice list?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSong(song); songToDelete = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // Full-screen image viewer
    songForImages?.let { song ->
        ImageViewerDialog(song = song, onDismiss = { songForImages = null })
    }

    // YouTube player bottom sheet
    songForYouTube?.let { song ->
        YouTubeBottomSheet(song = song, onDismiss = { songForYouTube = null })
    }

    // Add song dialog
    if (showAddDialog) {
        AddSongDialog(
            onDismiss = { showAddDialog = false },
            onSave    = { name, uris, url ->
                viewModel.addSong(name, uris, url, context)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Practice Songs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface)
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, "Menu", tint = OnDarkSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick         = { showAddDialog = true },
                containerColor  = AmberGold,
                contentColor    = OnDarkPrimary,
                shape           = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, "Add Song")
            }
        }
    ) { padding ->
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.MusicNote, null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No songs yet.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    Text("Tap + to add a practice song.", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongCard(
                        song      = song,
                        onView    = { songForImages  = song },
                        onPlay    = { songForYouTube = song },
                        onDelete  = { songToDelete   = song }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

// ── Song Card ─────────────────────────────────────────────────────────────────
@Composable
private fun SongCard(
    song: Song,
    onView: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val paths = song.imagePaths.split(",").filter { it.isNotBlank() }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = DarkCard,
        shape    = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column {
            // Gradient stripe
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(listOf(AmberGold, ElectricBlue)))
            )
            Row(
                modifier          = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                val bmp = remember(paths.firstOrNull()) {
                    paths.firstOrNull()?.let {
                        try { BitmapFactory.decodeFile(it) } catch (e: Exception) { null }
                    }
                }
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                        .background(DarkCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.MusicNote, null, tint = TextTertiary, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(song.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (paths.isNotEmpty()) "${paths.size} image(s)" else "No images",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    if (song.youtubeUrl.isNotBlank()) {
                        Text("YouTube link available", style = MaterialTheme.typography.bodySmall,
                            color = ElectricBlueLight)
                    }
                }

                // Action buttons
                IconButton(onClick = onView, enabled = paths.isNotEmpty()) {
                    Icon(Icons.Filled.Image, "View",
                        tint = if (paths.isNotEmpty()) AmberGold else TextTertiary)
                }
                IconButton(onClick = onPlay, enabled = song.youtubeUrl.isNotBlank()) {
                    Icon(Icons.Filled.PlayCircle, "Play",
                        tint = if (song.youtubeUrl.isNotBlank()) ElectricBlue else TextTertiary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ── Image Viewer ──────────────────────────────────────────────────────────────
@Composable
private fun ImageViewerDialog(song: Song, onDismiss: () -> Unit) {
    val paths   = song.imagePaths.split(",").filter { it.isNotBlank() }
    var pageIdx by remember { mutableStateOf(0) }

    // Accumulated horizontal drag — reset each gesture
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 80f

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .pointerInput(paths.size) {
                    detectHorizontalDragGestures(
                        onDragStart  = { dragAccum = 0f },
                        onDragCancel = { dragAccum = 0f },
                        onDragEnd    = {
                            when {
                                dragAccum < -swipeThresholdPx -> // swipe left → next (with wrap)
                                    pageIdx = (pageIdx + 1) % paths.size
                                dragAccum > swipeThresholdPx  -> // swipe right → prev (with wrap)
                                    pageIdx = (pageIdx - 1 + paths.size) % paths.size
                            }
                            dragAccum = 0f
                        }
                    ) { _, delta -> dragAccum += delta }
                },
            contentAlignment = Alignment.Center
        ) {
            // Current image
            val bmp = remember(pageIdx) {
                try { BitmapFactory.decodeFile(paths[pageIdx]) } catch (e: Exception) { null }
            }
            if (bmp != null) {
                Image(
                    bmp.asImageBitmap(), null,
                    modifier     = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Cannot load image", color = TextSecondary)
            }

            // Prev / Next arrow buttons (still shown for visibility)
            if (paths.size > 1) {
                Row(
                    modifier              = Modifier.fillMaxWidth().align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick  = { pageIdx = (pageIdx - 1 + paths.size) % paths.size },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, "Prev",
                            tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(36.dp))
                    }
                    IconButton(
                        onClick  = { pageIdx = (pageIdx + 1) % paths.size },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.ChevronRight, "Next",
                            tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(36.dp))
                    }
                }
                // Dot indicator
                Row(
                    modifier              = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paths.indices.forEach { i ->
                        Box(
                            modifier = Modifier.size(if (i == pageIdx) 10.dp else 7.dp).clip(CircleShape)
                                .background(if (i == pageIdx) AmberGold else Color.White.copy(0.35f))
                        )
                    }
                }
            }

            // Close button
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Page counter
            if (paths.size > 1) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    color    = Color.Black.copy(0.55f),
                    shape    = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${pageIdx + 1} / ${paths.size}",
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// ── YouTube Player ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YouTubeBottomSheet(song: Song, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val videoId    = extractYouTubeId(song.youtubeUrl)

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = DarkCard,
        dragHandle        = {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(DarkOutline))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                song.name,
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color    = OnDarkSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (videoId != null) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    factory  = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 10; K) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/124.0.0.0 Mobile Safari/537.36"
                            settings.javaScriptEnabled                = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.domStorageEnabled                = true
                            settings.loadWithOverviewMode             = true
                            settings.useWideViewPort                  = true
                            settings.mixedContentMode                 =
                                android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            webChromeClient = WebChromeClient()
                            // Load as an HTML page with youtube-nocookie.com as base URL.
                            // This makes the embed believe it is hosted on an allowed
                            // origin, fixing Error 153 / 'Video unavailable' blocks.
                            val html = """
                                <!DOCTYPE html><html>
                                <head><meta name="viewport" content="width=device-width,initial-scale=1">
                                <style>*{margin:0;padding:0;background:#000}
                                iframe{width:100%;height:100vh;border:0}</style></head>
                                <body>
                                <iframe src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&rel=0&playsinline=1&modestbranding=1&enablejsapi=1"
                                    allow="autoplay; fullscreen; encrypted-media"
                                    allowfullscreen></iframe>
                                </body></html>
                            """.trimIndent()
                            loadDataWithBaseURL(
                                "https://www.youtube-nocookie.com",
                                html, "text/html", "utf-8", null
                            )
                        }
                    }
                )
            } else {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("Invalid YouTube URL", color = TextSecondary)
                }
            }
        }
    }
}

// ── Add Song Dialog ───────────────────────────────────────────────────────────
@Composable
private fun AddSongDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<android.net.Uri>, String) -> Unit
) {
    var name         by remember { mutableStateOf("") }
    var youtubeUrl   by remember { mutableStateOf("") }
    val selectedUris = remember { mutableStateListOf<android.net.Uri>() }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedUris.addAll(uris) }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(),
            shape    = RoundedCornerShape(20.dp),
            color    = DarkCard
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add Practice Song",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnDarkSurface)

                // Song name
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Song Name") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor    = AmberGold,
                        unfocusedBorderColor  = DarkOutline,
                        focusedContainerColor   = DarkCardElevated,
                        unfocusedContainerColor = DarkCardElevated,
                        cursorColor           = AmberGold,
                        focusedTextColor      = OnDarkSurface,
                        unfocusedTextColor    = OnDarkSurface,
                        focusedLabelColor     = AmberGold
                    )
                )

                // YouTube URL
                OutlinedTextField(
                    value         = youtubeUrl,
                    onValueChange = { youtubeUrl = it },
                    label         = { Text("YouTube URL") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor    = ElectricBlue,
                        unfocusedBorderColor  = DarkOutline,
                        focusedContainerColor   = DarkCardElevated,
                        unfocusedContainerColor = DarkCardElevated,
                        cursorColor           = ElectricBlue,
                        focusedTextColor      = OnDarkSurface,
                        unfocusedTextColor    = OnDarkSurface,
                        focusedLabelColor     = ElectricBlue
                    )
                )

                // Images row
                Column {
                    Text("Images", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(selectedUris) { i, uri ->
                            val ctx = LocalContext.current
                            val bmp = remember(uri) {
                                try {
                                    ctx.contentResolver.openInputStream(uri)?.use {
                                        BitmapFactory.decodeStream(it)
                                    }
                                } catch (e: Exception) { null }
                            }
                            Box(modifier = Modifier.size(64.dp)) {
                                if (bmp != null) {
                                    Image(bmp.asImageBitmap(), null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop)
                                }
                                IconButton(
                                    onClick  = { selectedUris.removeAt(i) },
                                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Filled.Close, "Remove", tint = Color.White,
                                        modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                    .background(DarkCardElevated).clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, "Add image",
                                    tint = AmberGold, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                // Buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("Cancel") }
                    Button(
                        onClick  = { if (name.isNotBlank()) onSave(name, selectedUris.toList(), youtubeUrl) },
                        modifier = Modifier.weight(1f),
                        enabled  = name.isNotBlank(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = OnDarkPrimary)
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
