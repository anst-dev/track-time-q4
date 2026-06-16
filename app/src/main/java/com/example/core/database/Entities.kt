package com.example.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val quadrant: Int, // 1 = Urgent-Important, 2 = Important-NotUrgent, 3 = Urgent-NotImportant, 4 = NotUrgent-NotImportant
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val status: String = "PENDING", // PENDING, ACTIVE, COMPLETED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val date: String, // YYYY-MM-DD
    val isSynced: Boolean = false
)

@Entity(
    tableName = "time_logs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class TimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val startTime: Long, // Epoch ms
    val endTime: Long? = null, // Epoch ms
    val durationSeconds: Long = 0L,
    val isSynced: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val quadrant: Int = 4, // quadrant reference if associated
    val date: String, // YYYY-MM-DD
    val isSynced: Boolean = false
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncTime: Long,
    val status: String, // "Thành công", "Thất bại"
    val details: String
)

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt DESC")
    fun getTasksForDateFlow(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE quadrant = :quadrant ORDER BY createdAt DESC")
    fun getTasksForQuadrantFlow(quadrant: Int): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET actualMinutes = actualMinutes + :addMinutes WHERE id = :id")
    suspend fun incrementActualMinutes(id: Long, addMinutes: Int)
}

@Dao
interface TimeLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeLog(log: TimeLogEntity): Long

    @Update
    suspend fun updateTimeLog(log: TimeLogEntity)

    @Query("SELECT * FROM time_logs WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getTimeLogsForTaskFlow(taskId: Long): Flow<List<TimeLogEntity>>

    @Query("SELECT * FROM time_logs WHERE taskId = :taskId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveTimeLogForTask(taskId: Long): TimeLogEntity?

    @Query("SELECT * FROM time_logs WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveTimeLog(): TimeLogEntity?

    @Query("SELECT * FROM time_logs ORDER BY startTime DESC")
    fun getAllTimeLogsFlow(): Flow<List<TimeLogEntity>>

    @Query("SELECT tl.* FROM time_logs tl INNER JOIN tasks t ON tl.taskId = t.id WHERE t.date = :date")
    fun getTimeLogsForDateFlow(date: String): Flow<List<TimeLogEntity>>

    @Query("DELETE FROM time_logs WHERE id = :id")
    suspend fun deleteTimeLog(id: Long)
}

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE date = :date ORDER BY createdAt DESC")
    fun getNotesForDateFlow(date: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?
}

@Dao
interface SyncLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity): Long

    @Query("SELECT * FROM sync_logs ORDER BY syncTime DESC")
    fun getAllSyncLogsFlow(): Flow<List<SyncLogEntity>>
}
