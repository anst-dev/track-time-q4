package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.*
import com.example.ui.TimeTrackerViewModel
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TimeTrackerViewModel,
    onQuadrantClick: (Int) -> Unit
) {
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val activeLog by viewModel.activeTimeLog.collectAsState()
    val currentTask by viewModel.currentActiveTask.collectAsState()
    val tickerSeconds by viewModel.timerSeconds.collectAsState()
    val logs by viewModel.allTimeLogs.collectAsState()
    val totalSeconds by viewModel.totalTrackedSecondsToday.collectAsState(0L)
    val selectedDate by viewModel.selectedDate.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Context / Date Selector headers ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightSurface)
                    .clickable { /* Fast triggers */ }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Chế độ",
                    tint = PriorityGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Chế độ: Thao tác nhanh",
                    color = OnSurfaceNeutral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Thao tác nhanh",
                    tint = OnSurfaceNeutral,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightSurface)
                    .clickable { /* Choose Date logic if expanded */ }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Chọn Ngày",
                    tint = OnSurfaceNeutral,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Hôm nay: $selectedDate",
                    color = OnSurfaceNeutral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = OnSurfaceNeutral,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- 4 QUADRANTS ---
        // Quadrant 1, 2, 3, 4 Card Builders
        for (q in 1..4) {
            val qColor = when (q) {
                1 -> PriorityRed
                2 -> PriorityGreen
                3 -> PriorityOrange
                else -> PriorityGrey
            }
            val qTitle = when (q) {
                1 -> "Quan trọng – Khẩn cấp"
                2 -> "Quan trọng – Không khẩn cấp"
                3 -> "Không quan trọng – Khẩn cấp"
                else -> "Không quan trọng – Không khẩn cấp"
            }
            val qSub = when (q) {
                1 -> "Làm ngay"
                2 -> "Lên kế hoạch"
                3 -> "Ủy thác"
                else -> "Loại bỏ / Thư giãn"
            }

            // Time calculation inside this quadrant
            val isActive = activeLog != null && currentTask?.quadrant == q
            val timeString = if (isActive) {
                formatDuration(tickerSeconds)
            } else {
                // Sum actual time of tasks in this quadrant today (seconds)
                val qSec = logs.filter { it.isSynced == false }.sumOf { log ->
                    val task = tasks.firstOrNull { it.id == log.taskId }
                    if (task?.quadrant == q && log.endTime != null) log.durationSeconds else 0L
                }
                val taskAccumulated = tasks.filter { it.quadrant == q }.sumOf { it.actualMinutes * 60L }
                formatDuration((qSec + taskAccumulated).coerceAtLeast(0L))
            }

            QuadrantCard(
                qNum = q,
                title = qTitle,
                subtitle = qSub,
                color = qColor,
                timeText = timeString,
                isRunning = isActive,
                statusText = if (isActive) "Đang làm..." else "Chờ",
                onTogglePlay = {
                    // Toggle timer of first task in this quadrant, or find active task, or open picker
                    val qTasks = tasks.filter { it.quadrant == q && it.status != "COMPLETED" }
                    val activeTaskId = qTasks.firstOrNull()?.id
                    if (activeTaskId != null) {
                        viewModel.toggleTimerForTask(activeTaskId)
                    } else {
                        // Create quick temporary task and toggle
                        viewModel.createTask(
                            title = "Công việc mới Q$q",
                            description = "Nhiệm vụ tự động tạo trong Ma trận $qTitle",
                            quadrant = q,
                            estimatedMinutes = 30,
                            startImmediately = true
                        )
                    }
                },
                onCardClick = { onQuadrantClick(q) }
            )
        }

        // --- TOTAL DAILY TIME ALLOCATIONS ---
        DailySummarySection(totalSeconds, tasks, logs)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuadrantCard(
    qNum: Int,
    title: String,
    subtitle: String,
    color: Color,
    timeText: String,
    isRunning: Boolean,
    statusText: String,
    onTogglePlay: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_q$qNum")
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = qNum.toString(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Options/More icon
                IconButton(onClick = onCardClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Tác vụ",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontFamily = JetBrainsMonoFont, // tabular format mono support
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRunning) {
                        // Drawing a running pulse wave representation
                        PulseGraphCanvas(modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Chạy",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulseGraphCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()

        path.moveTo(8.dp.toPx(), height / 2)

        // Draw a simulated heartbeat graph representing dynamic flow live!
        val step = 2.dp.toPx()
        var x = 8.dp.toPx()
        while (x < width - 8.dp.toPx()) {
            val progress = (x - 8.dp.toPx()) / (width - 16.dp.toPx())
            // Emulate heart pulse (amplitude peaks in center, dies off at edges)
            val envelope = sin(progress * Math.PI).toFloat()
            val wave = sin((progress * 4f * Math.PI) - phase).toFloat()
            val y = (height / 2) + wave * envelope * 12.dp.toPx()

            path.lineTo(x, y)
            x += step
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun DailySummarySection(
    totalSeconds: Long,
    tasks: List<TaskEntity>,
    logs: List<TimeLogEntity>
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_section")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Tổng thời gian hôm nay",
                color = OnSurfaceNeutral,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (q in 1..4) {
                    val qColor = when (q) {
                        1 -> PriorityRed
                        2 -> PriorityGreen
                        3 -> PriorityOrange
                        else -> PriorityGrey
                    }
                    val qLabelSingle = when (q) {
                        1 -> "Khẩn cấp"
                        2 -> "Quan trọng"
                        3 -> "Khẩn cấp"
                        else -> "Không khẩn cấp"
                    }
                    val qLabelComplement = when (q) {
                        1 -> "Quan trọng"
                        2 -> "Không khẩn cấp"
                        3 -> "Không quan trọng"
                        else -> "Không quan trọng"
                    }

                    // Calculation
                    val qSec = logs.filter { it.isSynced == false }.sumOf { log ->
                        val task = tasks.firstOrNull { it.id == log.taskId }
                        if (task?.quadrant == q && log.endTime != null) log.durationSeconds else 0L
                    }
                    val taskAccumulated = tasks.filter { it.quadrant == q }.sumOf { it.actualMinutes * 60L }
                    val totalQSec = (qSec + taskAccumulated).coerceAtLeast(0L)

                    val percentage = if (totalSeconds > 0) {
                        (totalQSec * 100 / totalSeconds).toInt()
                    } else {
                        0
                    }

                    // Vertical Detail Columns
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (q < 4) 4.dp else 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(qColor)
                            )
                            Text(
                                text = qLabelSingle,
                                color = Color.Gray,
                                fontSize = 9.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Text(
                            text = qLabelComplement,
                            color = OnSurfaceNeutral,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Text(
                            text = formatDuration(totalQSec),
                            color = OnSurfaceNeutral,
                            fontFamily = JetBrainsMonoFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        Text(
                            text = "($percentage%)",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (q < 4) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(60.dp)
                                .background(Color.Gray.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}
