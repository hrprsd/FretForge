package com.fretforge.ui.group

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fretforge.data.AppDatabase
import com.fretforge.data.PracticeTask
import com.fretforge.data.PracticeGroup
import com.fretforge.repository.GroupRepository
import com.fretforge.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(db.practiceTaskDao())
    private val groupRepository = GroupRepository(db.practiceGroupDao())

    private val _tasks = MutableStateFlow<List<PracticeTask>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _groupName = MutableStateFlow("New Practice Group")
    val groupName = _groupName.asStateFlow()

    fun loadTasks(taskIdsStr: String, groupId: Long) {
        viewModelScope.launch {
            if (taskIdsStr.isEmpty()) return@launch
            val ids = taskIdsStr.split(",").mapNotNull { it.toIntOrNull() }
            val loadedTasks = taskRepository.getTasksByIds(ids)
            
            // Reorder to match the initial input order
            val orderedTasks = ids.mapNotNull { id -> loadedTasks.find { it.id == id } }
            _tasks.value = orderedTasks

            if (groupId > 0L) {
                // Fetch group name if editing
                groupRepository.allGroups.collect { groups ->
                    groups.find { it.groupId == groupId }?.let { group ->
                        _groupName.value = group.name
                    }
                }
            }
        }
    }

    fun updateGroupName(name: String) {
        _groupName.value = name
    }

    fun moveTask(fromIndex: Int, toIndex: Int) {
        val current = _tasks.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val task = current.removeAt(fromIndex)
            current.add(toIndex, task)
            _tasks.value = current
        }
    }

    fun saveGroup(groupId: Long, onSaved: () -> Unit) {
        viewModelScope.launch {
            val idsStr = _tasks.value.joinToString(",") { it.id.toString() }
            val group = PracticeGroup(
                groupId = groupId,
                name = _groupName.value,
                taskIds = idsStr
            )
            groupRepository.insertGroup(group)
            onSaved()
        }
    }
}
