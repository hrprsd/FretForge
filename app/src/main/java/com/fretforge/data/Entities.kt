package com.fretforge.data

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
