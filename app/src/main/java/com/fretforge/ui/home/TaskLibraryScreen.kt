package com.fretforge.ui.home

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fretforge.data.PracticeTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLibraryScreen(navController: NavController, viewModel: TaskLibraryViewModel = viewModel()) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIds by viewModel.selectedTaskIds.collectAsState()
    val expandedParents by viewModel.expandedParents.collectAsState()
    val showBanner by viewModel.showRestoredBanner.collectAsState()

    val parents = tasks.filter { it.isParent || it.parentId == null }

    Scaffold(
        floatingActionButton = {
            if (selectedIds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            navController.navigate("practice/${viewModel.getSelectedIdsString()}")
                        },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Start") },
                        text = { Text("Start Practice") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            navController.navigate("group_review/${viewModel.getSelectedIdsString()}")
                        },
                        icon = { Icon(Icons.Filled.Add, contentDescription = "Review") },
                        text = { Text("Create Group") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search tasks...") },
                singleLine = true
            )

            if (showBanner) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Restored from your last session",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { viewModel.dismissBanner() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // space for FAB
            ) {
                items(parents, key = { it.id }) { parent ->
                    val children = tasks.filter { !it.isParent && it.parentId == parent.id }
                    
                    if (children.isEmpty()) {
                        // Standalone selectable parent
                        ChildTaskRow(
                            task = parent,
                            isSelected = selectedIds.contains(parent.id),
                            onSelect = { viewModel.toggleSelection(parent.id) }
                        )
                    } else {
                        val isExpanded = expandedParents.contains(parent.id)
                        val childIds = children.map { it.id }
                        val selectedCount = selectedIds.count { it in childIds }

                        ParentTaskRow(
                            task = parent,
                            isExpanded = isExpanded,
                            selectedChildCount = selectedCount,
                            onToggle = { viewModel.toggleParentExpansion(parent.id) }
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                children.forEach { child ->
                                    ChildTaskRow(
                                        task = child,
                                        isSelected = selectedIds.contains(child.id),
                                        onSelect = { viewModel.toggleSelection(child.id) }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (task.category.isNotEmpty()) {
                Text(
                    text = task.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        if (selectedChildCount > 0) {
            Badge(modifier = Modifier.padding(end = 8.dp)) {
                Text("$selectedChildCount selected")
            }
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand"
        )
    }
}

@Composable
fun ChildTaskRow(
    task: PracticeTask,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        task.imageFile?.let { imageFileName ->
            val context = LocalContext.current
            val bmp = remember(imageFileName) {
                try {
                    context.assets.open("images/$imageFileName").use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch(e: Exception) { null }
            }
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray))
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.name, style = MaterialTheme.typography.bodyLarge)
            if (task.description.isNotEmpty()) {
                Text(text = task.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Switch(
            checked = isSelected,
            onCheckedChange = { onSelect() }
        )
    }
}
