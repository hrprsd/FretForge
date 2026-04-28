package com.fretforge.repository

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
    suspend fun insertTask(task: PracticeTask): Long = dao.insertTask(task)
    suspend fun updateTask(task: PracticeTask) = dao.updateTask(task)
    suspend fun deleteTask(id: Int) = dao.deleteTaskById(id)
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
