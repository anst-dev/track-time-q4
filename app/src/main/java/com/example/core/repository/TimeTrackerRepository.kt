package com.example.core.repository

import com.example.core.database.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeTrackerRepository(
    private val taskDao: TaskDao,
    private val timeLogDao: TimeLogDao,
    private val noteDao: NoteDao,
    private val syncLogDao: SyncLogDao
) {
    // Current date in YYYY-MM-DD
    fun getCurrentFormattedDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // --- TASK CRUD ---
    fun getTasksForDateFlow(date: String): Flow<List<TaskEntity>> {
        return taskDao.getTasksForDateFlow(date)
    }

    fun getAllTasksFlow(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasksFlow()
    }

    suspend fun getTaskById(id: Long): TaskEntity? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    // --- TIME LOGS ---
    fun getAllTimeLogsFlow(): Flow<List<TimeLogEntity>> {
        return timeLogDao.getAllTimeLogsFlow()
    }

    fun getTimeLogsForDateFlow(date: String): Flow<List<TimeLogEntity>> {
        return timeLogDao.getTimeLogsForDateFlow(date)
    }

    suspend fun getActiveTimeLog(): TimeLogEntity? {
        return timeLogDao.getActiveTimeLog()
    }

    suspend fun getActiveTimeLogForTask(taskId: Long): TimeLogEntity? {
        return timeLogDao.getActiveTimeLogForTask(taskId)
    }

    suspend fun startTimer(taskId: Long): Long {
        // Stop any currently running timer first
        stopActiveTimer()

        val log = TimeLogEntity(
            taskId = taskId,
            startTime = System.currentTimeMillis()
        )
        val logId = timeLogDao.insertTimeLog(log)

        // Set task status to ACTIVE
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            taskDao.updateTask(task.copy(status = "ACTIVE", updatedAt = System.currentTimeMillis()))
        }
        return logId
    }

    suspend fun stopTimerForTask(taskId: Long) {
        val activeLog = timeLogDao.getActiveTimeLogForTask(taskId)
        if (activeLog != null) {
            val endTime = System.currentTimeMillis()
            val durationSeconds = (endTime - activeLog.startTime) / 1000L
            val updatedLog = activeLog.copy(
                endTime = endTime,
                durationSeconds = durationSeconds
            )
            timeLogDao.updateTimeLog(updatedLog)

            // Update task actual minutes (with ceiling for precision)
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                val newActualMinutes = task.actualMinutes + (durationSeconds / 60).toInt()
                taskDao.updateTask(
                    task.copy(
                        status = "PENDING", // wait, pending/idle
                        actualMinutes = newActualMinutes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun stopActiveTimer() {
        val activeLog = timeLogDao.getActiveTimeLog()
        if (activeLog != null) {
            val endTime = System.currentTimeMillis()
            val durationSeconds = (endTime - activeLog.startTime) / 1000L
            val updatedLog = activeLog.copy(
                endTime = endTime,
                durationSeconds = durationSeconds
            )
            timeLogDao.updateTimeLog(updatedLog)

            val task = taskDao.getTaskById(activeLog.taskId)
            if (task != null) {
                val newActualMinutes = task.actualMinutes + (durationSeconds / 60).toInt()
                taskDao.updateTask(
                    task.copy(
                        status = "PENDING",
                        actualMinutes = newActualMinutes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteTimeLog(id: Long) {
        timeLogDao.deleteTimeLog(id)
    }

    // --- NOTES CRUD ---
    fun getNotesForDateFlow(date: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesForDateFlow(date)
    }

    fun getAllNotesFlow(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotesFlow()
    }

    suspend fun getNoteById(id: Long): NoteEntity? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: NoteEntity): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    // --- SYNC LOGS ---
    fun getAllSyncLogsFlow(): Flow<List<SyncLogEntity>> {
        return syncLogDao.getAllSyncLogsFlow()
    }

    suspend fun addSyncLog(status: String, details: String) {
        syncLogDao.insertSyncLog(
            SyncLogEntity(
                syncTime = System.currentTimeMillis(),
                status = status,
                details = details
            )
        )
    }
}
