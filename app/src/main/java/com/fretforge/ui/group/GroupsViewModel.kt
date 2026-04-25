package com.fretforge.ui.group

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fretforge.data.AppDatabase
import com.fretforge.data.PracticeGroup
import com.fretforge.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repo = GroupRepository(db.practiceGroupDao())

    private val _groups = MutableStateFlow<List<PracticeGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    init {
        viewModelScope.launch {
            repo.allGroups.collect { list ->
                _groups.value = list
            }
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            repo.deleteGroup(groupId)
        }
    }
}
