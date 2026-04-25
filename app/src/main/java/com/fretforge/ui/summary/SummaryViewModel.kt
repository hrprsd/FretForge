package com.fretforge.ui.summary

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

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repo = SessionRepository(db.practiceSessionDao())

    private val _session = MutableStateFlow<PracticeSession?>(null)
    val session = _session.asStateFlow()

    private val _tasks = MutableStateFlow<List<PracticeSessionTask>>(emptyList())
    val tasks = _tasks.asStateFlow()

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            repo.allSessions.collect { sessions ->
                val s = sessions.find { it.sessionId == sessionId }
                _session.value = s
            }
        }
        viewModelScope.launch {
            repo.getSessionTasks(sessionId).collect { t ->
                _tasks.value = t
            }
        }
    }
}
