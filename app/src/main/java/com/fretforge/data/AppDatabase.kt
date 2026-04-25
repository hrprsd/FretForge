package com.fretforge.data

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
