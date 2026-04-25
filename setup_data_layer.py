import os

base_dir = r"C:\Users\Haraprasad\.gemini\antigravity\Projects\FretForge"

def write_file(path, content):
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

# Entities.kt
write_file("app/src/main/java/com/fretforge/data/Entities.kt", """package com.fretforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_tasks")
data class PracticeTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val category: String,
    val isParent: Boolean,
    val parentId: Int?,
    val imageFile: String?
)

@Entity(tableName = "practice_groups")
data class PracticeGroup(
    @PrimaryKey(autoGenerate = true) val groupId: Long = 0,
    val name: String,
    val taskIds: String // Comma separated IDs
)

@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val groupName: String,
    val startTimestamp: Long,
    val totalDurationSeconds: Long
)

@Entity(tableName = "practice_session_tasks")
data class PracticeSessionTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val taskId: Int,
    val taskName: String,
    val orderIndex: Int,
    val timeSpentSeconds: Long,
    val bpmUsed: Int
)
""")

# Daos.kt
write_file("app/src/main/java/com/fretforge/data/Daos.kt", """package com.fretforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeTaskDao {
    @Query("SELECT * FROM practice_tasks")
    fun getAllTasks(): Flow<List<PracticeTask>>
    
    @Query("SELECT * FROM practice_tasks WHERE id IN (:ids)")
    suspend fun getTasksByIds(ids: List<Int>): List<PracticeTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<PracticeTask>)

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
""")

# AppDatabase.kt
write_file("app/src/main/java/com/fretforge/data/AppDatabase.kt", """package com.fretforge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PracticeTask::class, PracticeGroup::class, PracticeSession::class, PracticeSessionTask::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun practiceTaskDao(): PracticeTaskDao
    abstract fun practiceGroupDao(): PracticeGroupDao
    abstract fun practiceSessionDao(): PracticeSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fret_forge_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
""")

# PreferencesManager.kt
write_file("app/src/main/java/com/fretforge/data/PreferencesManager.kt", """package com.fretforge.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fret_forge_prefs", Context.MODE_PRIVATE)

    fun saveLastUsedTaskIds(ids: List<Int>) {
        prefs.edit().putString("last_used_task_ids", ids.joinToString(",")).apply()
    }

    fun getLastUsedTaskIds(): List<Int> {
        val str = prefs.getString("last_used_task_ids", "") ?: ""
        if (str.isEmpty()) return emptyList()
        return str.split(",").mapNotNull { it.toIntOrNull() }
    }

    fun saveBpmForTask(taskId: Int, bpm: Int) {
        prefs.edit().putInt("bpm_task_$taskId", bpm).apply()
    }

    fun getBpmForTask(taskId: Int): Int {
        return prefs.getInt("bpm_task_$taskId", 60)
    }
}
""")

# Repository.kt
write_file("app/src/main/java/com/fretforge/repository/Repository.kt", """package com.fretforge.repository

import com.fretforge.data.PracticeTask
import com.fretforge.data.PracticeGroup
import com.fretforge.data.PracticeSession
import com.fretforge.data.PracticeSessionTask
import com.fretforge.data.PracticeTaskDao
import com.fretforge.data.PracticeGroupDao
import com.fretforge.data.PracticeSessionDao
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: PracticeTaskDao) {
    val allTasks: Flow<List<PracticeTask>> = dao.getAllTasks()

    suspend fun getTasksByIds(ids: List<Int>): List<PracticeTask> = dao.getTasksByIds(ids)
    
    suspend fun countTasks(): Int = dao.count()
    suspend fun insertTasks(tasks: List<PracticeTask>) = dao.insertAll(tasks)
}

class GroupRepository(private val dao: PracticeGroupDao) {
    val allGroups: Flow<List<PracticeGroup>> = dao.getAllGroups()
    
    suspend fun insertGroup(group: PracticeGroup): Long = dao.insertGroup(group)
    suspend fun deleteGroup(groupId: Long) = dao.deleteGroup(groupId)
}

class SessionRepository(private val dao: PracticeSessionDao) {
    val allSessions: Flow<List<PracticeSession>> = dao.getAllSessions()

    fun getSessionTasks(sessionId: Long): Flow<List<PracticeSessionTask>> = dao.getSessionTasks(sessionId)

    suspend fun insertSessionWithTasks(session: PracticeSession, tasks: List<PracticeSessionTask>) {
        val sessionId = dao.insertSession(session)
        val tasksWithSessionId = tasks.map { it.copy(sessionId = sessionId) }
        dao.insertSessionTasks(tasksWithSessionId)
    }
    
    suspend fun deleteSession(sessionId: Long) = dao.deleteSessionWithTasks(sessionId)
}
""")

print("Data layer set up complete.")
