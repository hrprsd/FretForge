package com.fretforge.ui.songs

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fretforge.data.AppDatabase
import com.fretforge.data.Song
import com.fretforge.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SongsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SongRepository(AppDatabase.getDatabase(application).songDao())

    val songs = repo.allSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSong(name: String, imageUris: List<Uri>, youtubeUrl: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedPaths = imageUris.mapIndexed { i, uri ->
                copyImageToInternal(context, uri, "${System.currentTimeMillis()}_$i")
            }.filterNotNull()
            repo.insert(
                Song(
                    name       = name,
                    imagePaths = savedPaths.joinToString(","),
                    youtubeUrl = youtubeUrl.trim()
                )
            )
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clean up images
            song.imagePaths.split(",").filter { it.isNotBlank() }.forEach { File(it).delete() }
            repo.delete(song.id)
        }
    }

    private fun copyImageToInternal(context: Context, uri: Uri, name: String): String? {
        return try {
            val dir = File(context.filesDir, "song_images").also { it.mkdirs() }
            val dest = File(dir, "$name.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        } catch (e: Exception) { null }
    }
}

fun extractYouTubeId(url: String): String? {
    val patterns = listOf(
        Regex("v=([a-zA-Z0-9_-]{11})"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Regex("embed/([a-zA-Z0-9_-]{11})")
    )
    for (p in patterns) {
        val match = p.find(url)
        if (match != null) return match.groupValues[1]
    }
    return null
}
