package com.fretforge.ui.practice

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hrprsd.fretforge.R
import com.fretforge.data.AppDatabase
import com.fretforge.data.PracticeSession
import com.fretforge.data.PracticeSessionTask
import com.fretforge.data.PracticeTask
import com.fretforge.data.PreferencesManager
import com.fretforge.repository.SessionRepository
import com.fretforge.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(db.practiceTaskDao())
    private val sessionRepository = SessionRepository(db.practiceSessionDao())
    private val prefs = PreferencesManager(application)

    private val soundPool: SoundPool
    private val clickNormalId: Int
    private val clickAccentId: Int

    private val _tasks = MutableStateFlow<List<PracticeTask>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    val currentTask get() = _tasks.value.getOrNull(_currentIndex.value)

    private val _bpm = MutableStateFlow(60)
    val bpm = _bpm.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _beatStates = MutableStateFlow(listOf(1, 0, 0, 0)) // 0: Normal, 1: Accent, 2: Muted
    val beatStates = _beatStates.asStateFlow()

    private val _activeBeatIndex = MutableStateFlow(-1)
    val activeBeatIndex = _activeBeatIndex.asStateFlow()

    private val _taskTimeSeconds = MutableStateFlow(0L)
    val taskTimeSeconds = _taskTimeSeconds.asStateFlow()

    private val _totalSessionTimeSeconds = MutableStateFlow(0L)
    val totalSessionTimeSeconds = _totalSessionTimeSeconds.asStateFlow()

    private var metronomeJob: Job? = null
    private var stopwatchJob: Job? = null
    
    private var sessionStartTime: Long = 0
    private var taskStartTime: Long = 0
    private val completedTasks = mutableListOf<PracticeSessionTask>()

    init {
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(audioAttr).build()
        clickNormalId = soundPool.load(application, R.raw.click_normal, 1)
        clickAccentId = soundPool.load(application, R.raw.click_accent, 1)
    }

    fun loadTasks(taskIdsStr: String) {
        viewModelScope.launch {
            if (taskIdsStr.isEmpty()) return@launch
            val ids = taskIdsStr.split(",").mapNotNull { it.toIntOrNull() }
            val loaded = taskRepository.getTasksByIds(ids)
            _tasks.value = ids.mapNotNull { id -> loaded.find { it.id == id } }
            
            if (_tasks.value.isNotEmpty()) {
                sessionStartTime = System.currentTimeMillis()
                loadCurrentTaskSettings()
                startStopwatch()
            }
        }
    }

    private fun loadCurrentTaskSettings() {
        taskStartTime = SystemClock.elapsedRealtime()
        _taskTimeSeconds.value = 0L
        val task = currentTask ?: return
        
        val savedBpm = prefs.getBpmForTask(task.id)
        _bpm.value = if (savedBpm > 0) savedBpm else 50
        
        startMetronome()
    }

    private fun startStopwatch() {
        stopwatchJob?.cancel()
        val startTotal = SystemClock.elapsedRealtime() - (_totalSessionTimeSeconds.value * 1000)
        stopwatchJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                _totalSessionTimeSeconds.value = (now - startTotal) / 1000
                _taskTimeSeconds.value = (now - taskStartTime) / 1000
            }
        }
    }

    fun toggleMetronome() {
        if (_isPlaying.value) {
            stopMetronome()
        } else {
            startMetronome()
        }
    }

    private fun startMetronome() {
        _isPlaying.value = true
        metronomeJob?.cancel()
        metronomeJob = viewModelScope.launch {
            var nextClickTime = SystemClock.elapsedRealtime()
            var currentBeat = 0
            while (isActive) {
                val delayTime = nextClickTime - SystemClock.elapsedRealtime()
                if (delayTime > 0) delay(delayTime)

                _activeBeatIndex.value = currentBeat
                
                val state = _beatStates.value[currentBeat]
                if (state == 0) soundPool.play(clickNormalId, 1f, 1f, 1, 0, 1f)
                else if (state == 1) soundPool.play(clickAccentId, 1f, 1f, 1, 0, 1f)
                // if 2 (muted), do nothing

                currentBeat = (currentBeat + 1) % 4
                
                val intervalMs = (60000.0 / _bpm.value).toLong()
                nextClickTime += intervalMs
            }
        }
    }

    private fun stopMetronome() {
        _isPlaying.value = false
        _activeBeatIndex.value = -1
        metronomeJob?.cancel()
    }

    fun updateBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(50, 500)
        currentTask?.let { prefs.saveBpmForTask(it.id, _bpm.value) }
        if (_isPlaying.value) {
            startMetronome() // restart with new timing
        }
    }

    fun toggleBeatState(index: Int) {
        val currentList = _beatStates.value.toMutableList()
        currentList[index] = (currentList[index] + 1) % 3
        _beatStates.value = currentList
    }

    fun nextTask() {
        recordCurrentTask()
        if (_currentIndex.value < _tasks.value.lastIndex) {
            _currentIndex.value += 1
            loadCurrentTaskSettings()
        }
    }

    fun endSession(onComplete: (Long) -> Unit) {
        recordCurrentTask()
        stopMetronome()
        stopwatchJob?.cancel()
        
        viewModelScope.launch {
            val session = PracticeSession(
                groupName = "Practice Session",
                startTimestamp = sessionStartTime,
                totalDurationSeconds = _totalSessionTimeSeconds.value
            )
            sessionRepository.insertSessionWithTasks(session, completedTasks)
            // Retrieve latest session ID safely
            val allSessions = sessionRepository.allSessions
            allSessions.collect { sessionsList ->
                sessionsList.firstOrNull()?.sessionId?.let { 
                    onComplete(it) 
                }
            }
        }
    }

    private fun recordCurrentTask() {
        currentTask?.let { task ->
            completedTasks.add(
                PracticeSessionTask(
                    sessionId = 0, // Assigned in repo
                    taskId = task.id,
                    taskName = task.name,
                    orderIndex = _currentIndex.value,
                    timeSpentSeconds = _taskTimeSeconds.value,
                    bpmUsed = _bpm.value
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPool.release()
    }
}
