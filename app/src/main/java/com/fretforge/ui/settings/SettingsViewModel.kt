package com.fretforge.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fretforge.data.AppDatabase
import com.fretforge.data.PracticeTask
import com.fretforge.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TaskRepository(AppDatabase.getDatabase(application).practiceTaskDao())

    val tasks = repo.allTasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun addTask(task: PracticeTask) = viewModelScope.launch {
        repo.insertTask(task)
    }

    fun updateTask(task: PracticeTask) = viewModelScope.launch {
        repo.updateTask(task)
    }

    fun deleteTask(id: Int) = viewModelScope.launch {
        repo.deleteTask(id)
    }

    /** Copies a user-picked image URI into app-private storage and returns the path. */
    fun saveTaskImage(context: Context, uri: Uri): String? {
        return try {
            val dir  = File(context.filesDir, "task_images").also { it.mkdirs() }
            val dest = File(dir, "${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        } catch (e: Exception) { null }
    }
}
