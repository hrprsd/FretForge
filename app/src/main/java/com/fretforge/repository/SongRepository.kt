package com.fretforge.repository

import com.fretforge.data.Song
import com.fretforge.data.SongDao
import kotlinx.coroutines.flow.Flow

class SongRepository(private val dao: SongDao) {
    val allSongs: Flow<List<Song>> = dao.getAllSongs()
    suspend fun insert(song: Song): Long = dao.insertSong(song)
    suspend fun delete(id: Long) = dao.deleteSong(id)
}
