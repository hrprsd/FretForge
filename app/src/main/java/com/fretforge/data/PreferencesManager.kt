package com.fretforge.data

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
