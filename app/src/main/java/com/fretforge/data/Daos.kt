package com.fretforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeTaskDao {
    @Query("SELECT * FROM practice_tasks")
    fun getAllTasks(): Flow<List<PracticeTask>>
    
    @Query("SELECT * FROM practice_tasks WHERE id IN (:ids)")
    suspend fun getTasksByIds(ids: List<Int>): List<PracticeTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<PracticeTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PracticeTask): Long

    @Update
    suspend fun updateTask(task: PracticeTask)

    @Query("DELETE FROM practice_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    @Query("SELECT COUNT(id) FROM practice_tasks")
    suspend fun count(): Int
}

@Dao
interface PracticeGroupDao {
    @Query("SELECT * FROM practice_groups")
    fun getAllGroups(): Flow<List<PracticeGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: PracticeGroup): Long
    
    @Query("DELETE FROM practice_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: Long)
}

@Dao
interface PracticeSessionDao {
    @Query("SELECT * FROM practice_sessions ORDER BY startTimestamp DESC")
    fun getAllSessions(): Flow<List<PracticeSession>>

    @Query("SELECT * FROM practice_session_tasks WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getSessionTasks(sessionId: Long): Flow<List<PracticeSessionTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionTasks(tasks: List<PracticeSessionTask>)
    
    @Query("DELETE FROM practice_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)
    
    @Query("DELETE FROM practice_session_tasks WHERE sessionId = :sessionId")
    suspend fun deleteSessionTasks(sessionId: Long)
    
    @Transaction
    suspend fun deleteSessionWithTasks(sessionId: Long) {
        deleteSession(sessionId)
        deleteSessionTasks(sessionId)
    }
}

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY id DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: Long)
}
