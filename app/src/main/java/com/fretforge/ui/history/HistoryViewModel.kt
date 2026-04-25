package com.fretforge.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fretforge.data.AppDatabase
import com.fretforge.data.PracticeSession
import com.fretforge.data.PracticeSessionTask
import com.fretforge.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repo = SessionRepository(db.practiceSessionDao())

    private val _sessions = MutableStateFlow<List<PracticeSession>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _sessionTasks = MutableStateFlow<Map<Long, List<PracticeSessionTask>>>(emptyMap())
    val sessionTasks = _sessionTasks.asStateFlow()

    init {
        viewModelScope.launch {
            repo.allSessions.collect { list ->
                _sessions.value = list
                // Fetch tasks for each session
                list.forEach { session ->
                    launch {
                        repo.getSessionTasks(session.sessionId).collect { tasks ->
                            val currentMap = _sessionTasks.value.toMutableMap()
                            currentMap[session.sessionId] = tasks
                            _sessionTasks.value = currentMap
                        }
                    }
                }
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repo.deleteSession(sessionId)
        }
    }
}
