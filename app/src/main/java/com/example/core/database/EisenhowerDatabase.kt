package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        TimeLogEntity::class,
        NoteEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EisenhowerDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun timeLogDao(): TimeLogDao
    abstract fun noteDao(): NoteDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: EisenhowerDatabase? = null

        fun getDatabase(context: Context): EisenhowerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EisenhowerDatabase::class.java,
                    "eisenhower_database.db"
                )
                .fallbackToDestructiveMigration() // safe for local dev template
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
