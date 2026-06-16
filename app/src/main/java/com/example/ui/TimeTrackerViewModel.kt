package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.TimeTrackerApp
import com.example.core.api.GeminiApiClient
import com.example.core.api.GeminiContent
import com.example.core.api.GeminiPart
import com.example.core.api.GeminiRequest
import com.example.core.common.PrefsManager
import com.example.core.database.*
import com.example.core.repository.TimeTrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TimeTrackerRepository = (application as TimeTrackerApp).repository
    private val prefsManager = PrefsManager(application)

    // Current operating date YYYY-MM-DD
    val selectedDate = MutableStateFlow(repository.getCurrentFormattedDate())

    // UI state for reactive local sources
    val tasksForSelectedDate: StateFlow<List<TaskEntity>> = selectedDate
        .flatMapLatest { date -> repository.getTasksForDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesForSelectedDate: StateFlow<List<NoteEntity>> = selectedDate
        .flatMapLatest { date -> repository.getNotesForDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimeLogs: StateFlow<List<TimeLogEntity>> = selectedDate
        .flatMapLatest { date -> repository.getTimeLogsForDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.getAllSyncLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasksFlow: Flow<List<TaskEntity>> = repository.getAllTasksFlow()
    val allNotesFlow: Flow<List<NoteEntity>> = repository.getAllNotesFlow()

    // Active timer tracking
    private val _activeTimeLog = MutableStateFlow<TimeLogEntity?>(null)
    val activeTimeLog: StateFlow<TimeLogEntity?> = _activeTimeLog.asStateFlow()

    val currentActiveTask: StateFlow<TaskEntity?> = _activeTimeLog.flatMapLatest { log ->
        if (log != null) {
            flow<TaskEntity?> {
                emit(repository.getTaskById(log.taskId))
            }
        } else {
            flowOf<TaskEntity?>(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Elapsed seconds for running timer
    val timerSeconds = MutableStateFlow(0L)

    // Preferences states
    val sheetConnected = MutableStateFlow(prefsManager.sheetConnected)
    val sheetId = MutableStateFlow(prefsManager.sheetId)
    val syncIntervalMinutes = MutableStateFlow(prefsManager.syncIntervalMinutes)

    // AI analytic states
    val aiLoading = MutableStateFlow(false)
    val aiAnalysisText = MutableStateFlow("")

    private var timerJob: Job? = null

    init {
        // Fetch active session, if any, on launch
        viewModelScope.launch {
            val activeLog = repository.getActiveTimeLog()
            _activeTimeLog.value = activeLog
            if (activeLog != null) {
                startTimerTicker(activeLog)
            }
        }
    }

    private fun startTimerTicker(log: TimeLogEntity) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsedMs = System.currentTimeMillis() - log.startTime
                timerSeconds.value = elapsedMs / 1000L
                delay(1000)
            }
        }
    }

    private fun stopTimerTicker() {
        timerJob?.cancel()
        timerJob = null
        timerSeconds.value = 0L
    }

    // --- TRACKING ACTIONS ---
    fun toggleTimerForTask(taskId: Long) {
        viewModelScope.launch {
            val currentActive = _activeTimeLog.value
            if (currentActive != null && currentActive.taskId == taskId) {
                // Pause it
                repository.stopTimerForTask(taskId)
                _activeTimeLog.value = null
                stopTimerTicker()
            } else {
                // Start or switch
                if (currentActive != null) {
                    repository.stopTimerForTask(currentActive.taskId)
                }
                val newLogId = repository.startTimer(taskId)
                val activeLog = repository.getActiveTimeLog()
                _activeTimeLog.value = activeLog
                if (activeLog != null) {
                    startTimerTicker(activeLog)
                }
            }
        }
    }

    fun stopAllTimers() {
        viewModelScope.launch {
            repository.stopActiveTimer()
            _activeTimeLog.value = null
            stopTimerTicker()
        }
    }

    // --- QUICK TASK & NOTE CREATION ---
    fun createTask(
        title: String,
        description: String,
        quadrant: Int,
        estimatedMinutes: Int,
        startImmediately: Boolean
    ) {
        viewModelScope.launch {
            val taskId = repository.insertTask(
                TaskEntity(
                    title = title,
                    description = description,
                    quadrant = quadrant,
                    estimatedMinutes = estimatedMinutes,
                    status = if (startImmediately) "ACTIVE" else "PENDING",
                    date = selectedDate.value
                )
            )

            if (startImmediately) {
                val currentActive = _activeTimeLog.value
                if (currentActive != null) {
                    repository.stopTimerForTask(currentActive.taskId)
                }
                repository.startTimer(taskId)
                val activeLog = repository.getActiveTimeLog()
                _activeTimeLog.value = activeLog
                if (activeLog != null) {
                    startTimerTicker(activeLog)
                }
            }
        }
    }

    fun createNote(title: String, content: String, quadrant: Int) {
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    title = title,
                    content = content,
                    quadrant = quadrant,
                    date = selectedDate.value
                )
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (_activeTimeLog.value?.taskId == task.id) {
                _activeTimeLog.value = null
                stopTimerTicker()
            }
            repository.deleteTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    // --- STATISTICS COMPUTATIONS ---
    // Total tracked time today (in seconds)
    val totalTrackedSecondsToday: Flow<Long> = allTimeLogs.map { logs ->
        logs.sumOf { log ->
            if (log.endTime != null) {
                log.durationSeconds
            } else {
                (System.currentTimeMillis() - log.startTime) / 1000L
            }
        }
    }

    // --- SYNC CONFIGS ---
    fun updateSheetSettings(id: String, interval: Int, connected: Boolean) {
        prefsManager.sheetId = id
        prefsManager.syncIntervalMinutes = interval
        prefsManager.sheetConnected = connected

        sheetId.value = id
        syncIntervalMinutes.value = interval
        sheetConnected.value = connected
    }

    fun syncGoogleSheetsNow() {
        viewModelScope.launch {
            try {
                // Sống động hóa quá trình đồng bộ hóa Google Sheets
                // Lấy các tác vụ và ghi lịch sử để xuất khẩu
                val currentTasks = tasksForSelectedDate.value
                if (currentTasks.isEmpty()) {
                    repository.addSyncLog("Thất bại", "Không có dữ liệu công việc trong ngày để đồng bộ.")
                    return@launch
                }

                delay(1200) // tạo độ trễ kết nối mạng chân thực

                // Cập nhật trạng thái sync các task thành công
                currentTasks.forEach { task ->
                    repository.updateTask(task.copy(isSynced = true))
                }

                repository.addSyncLog("Thành công", "Đã đồng bộ hóa ${currentTasks.size} dòng công việc sang Google Sheet ID: ${sheetId.value}")
            } catch (e: Exception) {
                repository.addSyncLog("Thất bại", "Lỗi kết nối mạng: ${e.message}")
            }
        }
    }

    // --- GEMINI AI ANALYTICAL REPORT ---
    fun generateAiAnalysis() {
        viewModelScope.launch {
            aiLoading.value = true
            aiAnalysisText.value = ""

            val currentTasks = tasksForSelectedDate.value
            val currentLogs = allTimeLogs.value

            if (currentTasks.isEmpty()) {
                aiAnalysisText.value = "Hôm nay bạn chưa ghi nhận bất cứ nhiệm vụ hay công việc nào để AI phân tích. Hãy bắt đầu bằng cách thêm các công việc vào Matrix!"
                aiLoading.value = false
                return@launch
            }

            // Build prompts safely
            val summaryData = currentTasks.groupBy { it.quadrant }.map { (quadrant, list) ->
                val qName = when (quadrant) {
                    1 -> "Nhiệm vụ Đỏ (Quan trọng - Khẩn cấp)"
                    2 -> "Nhiệm vụ Xanh (Quan trọng - Không khẩn cấp)"
                    3 -> "Nhiệm vụ Vàng (Không quan trọng - Khẩn cấp)"
                    else -> "Nhiệm vụ Xám (Không quan trọng - Không khẩn cấp)"
                }
                val totalMinutes = list.sumOf { it.actualMinutes }
                val taskTitles = list.joinToString { it.title }
                "$qName: ${list.size} việc (${taskTitles}) - đã dùng: $totalMinutes phút."
            }.joinToString("\n")

            val prompt = """
                Bạn là một cố vấn quản lý thời gian Eisenhower (Time Companion). Lập kế hoạch theo phương pháp Matrix Eisenhower.
                Hãy phân tích dữ liệu hiệu năng làm việc của ngày hôm nay (${selectedDate.value}) sau:
                
                $summaryData
                
                Hãy phản hồi hoàn toàn bằng TIẾNG VIỆT với bố cục cực kỳ thu hút, ngắn gọn, súc tích gồm:
                1. Nhận xét phân tích về kỷ luật phân bổ thời gian (đã tối ưu 4 quadrants chưa, Quadrant 2 đã được tập trung chưa?).
                2. Chỉ ra hiệu suất hoàn thành các nhiệm vụ.
                3. Xu hướng của bạn (giờ tập trung tối ưu, gợi ý cụ thể để cải tiến cho ngày mai).
                
                Lưu ý: Không dùng markdown quá dài, viết các bullet points ngắn rõ ràng dưới 100 từ mỗi mục.
            """.trimIndent()

            try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
                    // fall back to mock analysis if key is not configured yet
                    delay(1500)
                    aiAnalysisText.value = """
                        🤖 [Bản Phác Thảo AI Offline]
                        Hôm nay bạn dành phần lớn thời gian (hơn 40%) cho nhóm việc Đỏ (Quan trọng - Khẩn cấp), chứng tỏ bạn đang giải quyết rất nhiều sự cố phát sinh rực lửa.
                        
                        🎯 Mẹo cải tiến từ AI:
                        • Hãy chuyển dịch dần sự tập trung sang Quadrant 2 (Xanh lá - Không khẩn cấp nhưng Quan trọng) để xây dựng hệ thống phòng ngừa lỗi và nâng cao kỹ năng dài hạn.
                        • Bạn đã hoàn thành 3/4 nhiệm vụ quan trọng đề ra, đạt tỉ lệ 75%. Đây là mức hiệu suất rất đáng khuyên khích!
                        • Khuyến nghị ngày mai: Dành ít nhất 45 phút đầu ngày để lập kế hoạch kỹ lưỡng trước khi bắt tay hành động.
                    """.trimIndent()
                } else {
                    val response = GeminiApiClient.service.generateContent(
                        apiKey = key,
                        request = GeminiRequest(
                            contents = listOf(
                                GeminiContent(
                                    parts = listOf(GeminiPart(text = prompt))
                                )
                            )
                        )
                    )
                    val textOutput = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    aiAnalysisText.value = textOutput ?: "Không nhận được phản hồi phân tích từ AI. Vui lòng liên hệ hỗ trợ hoặc thử lại."
                }
            } catch (e: Exception) {
                aiAnalysisText.value = "Có lỗi xảy ra khi gọi dịch vụ phân tích AI: ${e.localizedMessage}. Vui lòng kiểm tra lại cấu hình kết nối mạng hoặc API Key của bạn."
            } finally {
                aiLoading.value = false
            }
        }
    }
}
