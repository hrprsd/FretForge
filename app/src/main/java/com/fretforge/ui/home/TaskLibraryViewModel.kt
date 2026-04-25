package com.fretforge.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fretforge.data.AppDatabase
import com.fretforge.data.PracticeTask
import com.fretforge.data.PreferencesManager
import com.fretforge.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TaskLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(db.practiceTaskDao())
    private val prefs = PreferencesManager(application)

    private val _allTasks = MutableStateFlow<List<PracticeTask>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTaskIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTaskIds = _selectedTaskIds.asStateFlow()

    private val _expandedParents = MutableStateFlow<Set<Int>>(emptySet())
    val expandedParents = _expandedParents.asStateFlow()

    private val _showRestoredBanner = MutableStateFlow(false)
    val showRestoredBanner = _showRestoredBanner.asStateFlow()

    private val _filteredTasks = MutableStateFlow<List<PracticeTask>>(emptyList())
    val filteredTasks = _filteredTasks.asStateFlow()

    init {
        // Observe DB Tasks
        taskRepository.allTasks.onEach { tasks ->
            _allTasks.value = tasks
            // Ensure child tasks match the pre-selected ids on first load
            if (tasks.isNotEmpty() && _selectedTaskIds.value.isEmpty()) {
                val lastIds = prefs.getLastUsedTaskIds().toSet()
                if (lastIds.isNotEmpty()) {
                    val validLastIds = lastIds.intersect(tasks.map { it.id }.toSet())
                    if (validLastIds.isNotEmpty()) {
                        _selectedTaskIds.value = validLastIds
                        _showRestoredBanner.value = true
                        
                        // Expand parents that contain selected children
                        val parentsToExpand = tasks.filter { it.id in validLastIds }.mapNotNull { it.parentId }.toSet()
                        _expandedParents.value = parentsToExpand
                    }
                }
            }
            updateFilteredTasks()
        }.launchIn(viewModelScope)

        // Combine search query and all tasks to produce filtered list
        _searchQuery.onEach {
            updateFilteredTasks()
        }.launchIn(viewModelScope)
    }

    private fun updateFilteredTasks() {
        val q = _searchQuery.value.trim().lowercase()
        val all = _allTasks.value
        if (q.isEmpty()) {
            _filteredTasks.value = all
        } else {
            // Keep matching parents or children, and ensure parents of matching children are included
            val matchingTasks = all.filter { 
                it.name.lowercase().contains(q) || it.category.lowercase().contains(q) 
            }.toSet()

            val parentsOfMatches = matchingTasks.mapNotNull { it.parentId }.toSet()
            val matchesWithParents = all.filter { it in matchingTasks || it.id in parentsOfMatches }
            _filteredTasks.value = matchesWithParents
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(taskId: Int) {
        val current = _selectedTaskIds.value.toMutableSet()
        if (current.contains(taskId)) {
            current.remove(taskId)
        } else {
            current.add(taskId)
        }
        _selectedTaskIds.value = current
        prefs.saveLastUsedTaskIds(current.toList())
    }

    fun toggleParentExpansion(parentId: Int) {
        val current = _expandedParents.value.toMutableSet()
        if (current.contains(parentId)) {
            current.remove(parentId)
        } else {
            current.add(parentId)
        }
        _expandedParents.value = current
    }

    fun dismissBanner() {
        _showRestoredBanner.value = false
    }

    fun getSelectedIdsString(): String {
        return _selectedTaskIds.value.joinToString(",")
    }
}
