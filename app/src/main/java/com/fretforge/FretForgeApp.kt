package com.fretforge

import android.app.Application
import com.fretforge.data.AppDatabase
import com.fretforge.data.PracticeTask
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class FretForgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val db = AppDatabase.getDatabase(this)
        
        CoroutineScope(Dispatchers.IO).launch {
            if (db.practiceTaskDao().count() == 0) {
                try {
                    val stream = assets.open("tasks.json")
                    val reader = InputStreamReader(stream)
                    val tasksArray = Gson().fromJson(reader, Array<PracticeTask>::class.java)
                    db.practiceTaskDao().insertAll(tasksArray.toList())
                    reader.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
