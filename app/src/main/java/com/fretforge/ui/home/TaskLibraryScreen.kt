package com.fretforge.ui.home

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeTask
import com.fretforge.ui.components.LocalDrawerState
import com.fretforge.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLibraryScreen(navController: NavController, viewModel: TaskLibraryViewModel = viewModel()) {
    val tasks        by viewModel.filteredTasks.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val selectedIds  by viewModel.selectedTaskIds.collectAsState()
    val expandedParents by viewModel.expandedParents.collectAsState()
    val showBanner   by viewModel.showRestoredBanner.collectAsState()
    val drawerState  = LocalDrawerState.current
    val scope        = rememberCoroutineScope()
    val parents = tasks.filter { it.isParent || it.parentId == null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedIds.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            navController.navigate("group_review/${viewModel.getSelectedIdsString()}")
                        },
                        icon    = { Icon(Icons.Filled.Add, contentDescription = "Review") },
                        text    = { Text("Create Group", fontWeight = FontWeight.SemiBold) },
                        containerColor = ElectricBlue,
                        contentColor   = OnDarkSecondary,
                        shape          = RoundedCornerShape(16.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            navController.navigate("practice/${viewModel.getSelectedIdsString()}")
                        },
                        icon    = { Icon(Icons.Filled.PlayArrow, contentDescription = "Start") },
                        text    = { Text("Start Practice", fontWeight = FontWeight.SemiBold) },
                        containerColor = AmberGold,
                        contentColor   = OnDarkPrimary,
                        shape          = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Branded Header ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AmberGoldDark.copy(alpha = 0.35f),
                                ElectricBlueDark.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = AmberGoldLight)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FretForge",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AmberGoldLight, ElectricBlueLight)
                                )
                            )
                        )
                        Text(
                            text = "Select tasks to practice",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }


            // ── Search Bar ─────────────────────────────────────────────
            OutlinedTextField(
                value       = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search tasks...", color = TextTertiary) },
                singleLine  = true,
                shape       = RoundedCornerShape(14.dp),
                colors      = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AmberGold,
                    unfocusedBorderColor = DarkOutline,
                    focusedContainerColor    = DarkCard,
                    unfocusedContainerColor  = DarkCard,
                    cursorColor              = AmberGold
                )
            )

            // ── Restore Banner ─────────────────────────────────────────
            if (showBanner) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color  = AmberGoldDark.copy(alpha = 0.25f),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Restored from your last session",
                            color = AmberGoldLight,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick  = { viewModel.dismissBanner() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = AmberGoldLight
                            )
                        }
                    }
                }
            }

            // ── Task List ──────────────────────────────────────────────
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = 4.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(parents, key = { it.id }) { parent ->
                    val children = tasks.filter { !it.isParent && it.parentId == parent.id }

                    if (children.isEmpty()) {
                        ChildTaskRow(
                            task       = parent,
                            isSelected = selectedIds.contains(parent.id),
                            onSelect   = { viewModel.toggleSelection(parent.id) }
                        )
                    } else {
                        val isExpanded    = expandedParents.contains(parent.id)
                        val childIds      = children.map { it.id }
                        val selectedCount = selectedIds.count { it in childIds }

                        ParentTaskRow(
                            task               = parent,
                            isExpanded         = isExpanded,
                            selectedChildCount = selectedCount,
                            onToggle           = { viewModel.toggleParentExpansion(parent.id) }
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                children.forEach { child ->
                                    ChildTaskRow(
                                        task       = child,
                                        isSelected = selectedIds.contains(child.id),
                                        onSelect   = { viewModel.toggleSelection(child.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentTaskRow(
    task: PracticeTask,
    isExpanded: Boolean,
    selectedChildCount: Int,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle),
        color  = DarkCard,
        shape  = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Amber left-accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AmberGold)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = task.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnDarkSurface
                )
                if (task.category.isNotEmpty()) {
                    Text(
                        text  = task.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (selectedChildCount > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AmberGoldDim
                ) {
                    Text(
                        text     = "$selectedChildCount selected",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = AmberGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector  = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun ChildTaskRow(
    task: PracticeTask,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val rowBg = if (isSelected) AmberGoldDim else DarkCard.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect),
        color = rowBg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            task.imageFile?.let { imageFileName ->
                val context = LocalContext.current
                val bmp = remember(imageFileName) {
                    try {
                        context.assets.open("images/$imageFileName").use {
                            BitmapFactory.decodeStream(it)
                        }
                    } catch (e: Exception) { null }
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardElevated)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text    = task.name,
                    style   = MaterialTheme.typography.bodyLarge,
                    color   = OnDarkSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text  = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Custom selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) AmberGold else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) AmberGold else DarkOutline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = OnDarkPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
