package com.fretforge.ui.settings

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fretforge.data.PracticeTask
import com.fretforge.ui.components.LocalDrawerState
import com.fretforge.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

// ── Entry point ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val drawerState = LocalDrawerState.current
    val scope       = rememberCoroutineScope()
    val tasks by vm.tasks.collectAsState()

    // Dialog state
    var showDialog   by remember { mutableStateOf(false) }
    var editingTask  by remember { mutableStateOf<PracticeTask?>(null) }
    var deleteTarget by remember { mutableStateOf<PracticeTask?>(null) }

    Scaffold(
        containerColor = Color(0xFF111118),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, "Menu", tint = OnDarkSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111118))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick         = { editingTask = null; showDialog = true },
                containerColor  = AmberGold,
                contentColor    = OnDarkPrimary,
                shape           = CircleShape,
                elevation       = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Filled.Add, "Add Task")
            }
        }
    ) { padding ->

        val grouped = tasks.groupBy { it.category }

        if (tasks.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (category, categoryTasks) ->
                    item(key = "header_$category") {
                        CategoryHeader(category = category)
                        Spacer(Modifier.height(6.dp))
                    }
                    items(categoryTasks, key = { it.id }) { task ->
                        TaskCard(
                            task     = task,
                            onEdit   = { editingTask = task; showDialog = true },
                            onDelete = { deleteTarget = task }
                        )
                    }
                    item(key = "spacer_$category") { Spacer(Modifier.height(10.dp)) }
                }
            }
        }
    }

    // ── Add / Edit dialog ────────────────────────────────────────────────────
    if (showDialog) {
        TaskFormDialog(
            existing  = editingTask,
            allTasks  = tasks,
            viewModel = vm,
            onDismiss = { showDialog = false },
            onSave    = { task ->
                if (task.id == 0) vm.addTask(task) else vm.updateTask(task)
                showDialog = false
            }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    deleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = DarkCard,
            title = {
                Text("Delete Task", color = OnDarkSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Remove \"${task.name}\" from your task library? This cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteTask(task.id); deleteTarget = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ── Category header ───────────────────────────────────────────────────────────
@Composable
private fun CategoryHeader(category: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(4.dp, 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AmberGold)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            category.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color         = AmberGold
            )
        )
    }
}

// ── Task card ─────────────────────────────────────────────────────────────────
@Composable
private fun TaskCard(
    task: PracticeTask,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(14.dp),
        color         = DarkCard,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Leading icon / image chip
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(listOf(AmberGoldDim, Color.Transparent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                val bmp = remember(task.imageFile) {
                    task.imageFile?.takeIf { File(it).exists() }?.let {
                        try { BitmapFactory.decodeFile(it) } catch (e: Exception) { null }
                    }
                }
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(), null,
                        modifier     = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        if (task.isParent) Icons.Filled.Folder else Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint     = AmberGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.name,
                    style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color    = OnDarkSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (task.isParent) {
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = ElectricBlueDim) {
                        Text(
                            "Category",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = ElectricBlueLight
                        )
                    }
                }
            }

            // Action buttons
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Add / Edit task dialog ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormDialog(
    existing:  PracticeTask?,
    allTasks:  List<PracticeTask>,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onSave:    (PracticeTask) -> Unit
) {
    val context = LocalContext.current
    val isEdit  = existing != null

    var name        by remember { mutableStateOf(existing?.name        ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var category    by remember { mutableStateOf(existing?.category    ?: "") }
    var isParent    by remember { mutableStateOf(existing?.isParent    ?: false) }
    var nameError   by remember { mutableStateOf(false) }

    // Image state
    var imageFilePath by remember { mutableStateOf(existing?.imageFile ?: "") }
    var previewBitmap by remember {
        mutableStateOf(
            existing?.imageFile?.takeIf { File(it).exists() }?.let {
                try { BitmapFactory.decodeFile(it) } catch (e: Exception) { null }
            }
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = viewModel.saveTaskImage(context, uri)
            if (saved != null) {
                imageFilePath = saved
                previewBitmap = try { BitmapFactory.decodeFile(saved) } catch (e: Exception) { null }
            }
        }
    }

    // Parent dropdown state
    val parentTasks = allTasks.filter { it.isParent && it.id != (existing?.id ?: 0) }
    var parentDropdownExpanded by remember { mutableStateOf(false) }
    var selectedParent by remember {
        mutableStateOf(
            existing?.parentId?.let { pid -> allTasks.find { it.id == pid } }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape          = RoundedCornerShape(20.dp),
            color          = DarkCard,
            tonalElevation = 8.dp,
            modifier       = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(AmberGoldDark, AmberGold))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isEdit) Icons.Filled.Edit else Icons.Filled.Add,
                            contentDescription = null,
                            tint     = OnDarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isEdit) "Edit Task" else "Add New Task",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnDarkSurface
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Image picker ─────────────────────────────────────────────
                Text("Task Image", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thumbnail / placeholder
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardElevated)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                previewBitmap!!.asImageBitmap(), null,
                                modifier     = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Replace overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Edit, "Change", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Icon(Icons.Filled.AddPhotoAlternate, "Add Image", tint = AmberGold, modifier = Modifier.size(28.dp))
                        }
                    }

                    Column {
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            shape   = RoundedCornerShape(10.dp),
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = AmberGoldDim,
                                contentColor   = AmberGold
                            )
                        ) {
                            Icon(Icons.Filled.Image, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (previewBitmap != null) "Replace Image" else "Choose Image",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        if (previewBitmap != null) {
                            TextButton(onClick = { previewBitmap = null; imageFilePath = "" }) {
                                Text("Remove", color = ErrorRed, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Name ─────────────────────────────────────────────────────
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    label         = { Text("Task Name *") },
                    isError       = nameError,
                    supportingText = if (nameError) {{ Text("Name is required", color = ErrorRed) }} else null,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = settingsFieldColors()
                )

                Spacer(Modifier.height(12.dp))

                // ── Category ─────────────────────────────────────────────────
                OutlinedTextField(
                    value         = category,
                    onValueChange = { category = it },
                    label         = { Text("Category") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = settingsFieldColors(),
                    placeholder   = { Text("e.g. Scales, Chords, Rhythm", color = TextTertiary) }
                )

                Spacer(Modifier.height(12.dp))

                // ── Description ───────────────────────────────────────────────
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description") },
                    minLines      = 2,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = settingsFieldColors()
                )

                Spacer(Modifier.height(14.dp))

                // ── Is parent toggle ──────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isParent) AmberGoldDim else DarkCardElevated
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Category / Parent",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isParent) AmberGold else OnDarkSurface
                            )
                            Text(
                                "Mark as a top-level category group",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked         = isParent,
                            onCheckedChange = {
                                isParent = it
                                if (it) selectedParent = null  // clear parent selection when becomes parent
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = OnDarkPrimary,
                                checkedTrackColor   = AmberGold,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkOutline
                            )
                        )
                    }
                }

                // ── Parent selector (only when NOT a parent) ──────────────────
                AnimatedVisibility(visible = !isParent && parentTasks.isNotEmpty()) {
                    Column {
                        Spacer(Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded        = parentDropdownExpanded,
                            onExpandedChange = { parentDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                readOnly      = true,
                                value         = selectedParent?.name ?: "None (top-level)",
                                onValueChange = {},
                                label         = { Text("Parent Category") },
                                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(parentDropdownExpanded) },
                                modifier      = Modifier.fillMaxWidth().menuAnchor(),
                                shape         = RoundedCornerShape(12.dp),
                                colors        = settingsFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded         = parentDropdownExpanded,
                                onDismissRequest = { parentDropdownExpanded = false },
                                modifier         = Modifier.background(DarkCardElevated)
                            ) {
                                // "None" option
                                DropdownMenuItem(
                                    text    = { Text("None (top-level)", color = TextSecondary) },
                                    onClick = { selectedParent = null; parentDropdownExpanded = false }
                                )
                                Divider(color = DarkOutline)
                                parentTasks.forEach { parent ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                parent.name,
                                                color = if (selectedParent?.id == parent.id) AmberGold else OnDarkSurface
                                            )
                                        },
                                        onClick = { selectedParent = parent; parentDropdownExpanded = false },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Folder, null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Buttons ──────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) { nameError = true; return@Button }
                            onSave(
                                PracticeTask(
                                    id          = existing?.id ?: 0,
                                    name        = name.trim(),
                                    description = description.trim(),
                                    category    = category.trim().ifBlank { "General" },
                                    isParent    = isParent,
                                    parentId    = if (isParent) null else selectedParent?.id,
                                    imageFile   = imageFilePath.ifBlank { null }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor   = OnDarkPrimary
                        )
                    ) {
                        Icon(
                            if (isEdit) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isEdit) "Save" else "Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LibraryMusic, null, tint = TextTertiary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("No tasks yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text("Tap the + button to add your first practice task.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary)
    }
}

// ── Shared field colours ──────────────────────────────────────────────────────
@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = AmberGold,
    unfocusedBorderColor    = DarkOutline,
    focusedLabelColor       = AmberGold,
    unfocusedLabelColor     = TextSecondary,
    cursorColor             = AmberGold,
    focusedTextColor        = OnDarkSurface,
    unfocusedTextColor      = OnDarkSurface,
    focusedContainerColor   = DarkCardElevated,
    unfocusedContainerColor = DarkCardElevated
)
