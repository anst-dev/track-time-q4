package com.example

import android.app.Application
import com.example.core.database.EisenhowerDatabase
import com.example.core.repository.TimeTrackerRepository

class TimeTrackerApp : Application() {

    lateinit var database: EisenhowerDatabase
        private set

    lateinit var repository: TimeTrackerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = EisenhowerDatabase.getDatabase(this)
        repository = TimeTrackerRepository(
            taskDao = database.taskDao(),
            timeLogDao = database.timeLogDao(),
            noteDao = database.noteDao(),
            syncLogDao = database.syncLogDao()
        )
    }

    companion object {
        lateinit var instance: TimeTrackerApp
            private set
    }
}
