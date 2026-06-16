package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.TimeLogEntity
import com.example.ui.TimeTrackerViewModel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: TimeTrackerViewModel,
    onBackClick: () -> Unit
) {
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val logs by viewModel.allTimeLogs.collectAsState()
    val totalSeconds by viewModel.totalTrackedSecondsToday.collectAsState(0L)
    val selectedDate by viewModel.selectedDate.collectAsState()

    var activeSubTab by remember { mutableStateOf("Tổng quan") } // Tổng quan, Chi tiết, Timeline, AI phân tích

    val scrollState = rememberScrollState()

    // Aggregate statistics
    val totalTasksCount = tasks.size
    val completedTasksCount = tasks.count { it.status == "COMPLETED" }
    val completionSuccessRate = if (totalTasksCount > 0) {
        (completedTasksCount * 100 / totalTasksCount)
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- Custom App Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Trở về",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Báo cáo – $selectedDate",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(onClick = { /* Pick date */ }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Chọn ngày",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { /* Share report */ }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Chia sẻ",
                        tint = Color.White
                    )
                }
            }
        }

        // --- Stats Cards Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "Tổng thời gian",
                value = formatDuration(totalSeconds),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Số task",
                value = "$totalTasksCount",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Hoàn thành",
                value = "$completionSuccessRate%",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Sub Navigation Tabs ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val subTabs = listOf("Tổng quan", "Chi tiết", "Timeline", "AI phân tích")
            subTabs.forEach { tab ->
                val isSelected = activeSubTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryBlue else Color.Transparent)
                        .clickable { activeSubTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Sub Tab Page Contents ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            when (activeSubTab) {
                "Tổng quan" -> {
                    OverviewSubPage(totalSeconds, tasks, logs)
                }
                "Chi tiết" -> {
                    DetailSubPage(tasks)
                }
                "Timeline" -> {
                    TimelineSubPage(tasks, logs)
                }
                "AI phân tích" -> {
                    AiAnalysisSubPage(viewModel)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = JetBrainsMonoFont,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OverviewSubPage(
    totalSeconds: Long,
    tasks: List<com.example.core.database.TaskEntity>,
    logs: List<TimeLogEntity>
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Phân bổ theo loại",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // DONUT CANVAS Representation
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val angles = remember(tasks, logs, totalSeconds) {
                        var currentSum = 0f
                        val list = mutableListOf<Float>()
                        for (q in 1..4) {
                            val qSec = logs.filter { it.isSynced == false }.sumOf { log ->
                                val task = tasks.firstOrNull { it.id == log.taskId }
                                if (task?.quadrant == q && log.endTime != null) log.durationSeconds else 0L
                            }
                            val taskAccumulated = tasks.filter { it.quadrant == q }.sumOf { it.actualMinutes * 60L }
                            val qTotal = (qSec + taskAccumulated).coerceAtLeast(0L)

                            val sweep = if (totalSeconds > 0) {
                                (qTotal.toFloat() / totalSeconds.toFloat()) * 360f
                            } else {
                                90f // default equal split
                            }
                            list.add(sweep)
                        }
                        list
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val colors = listOf(PriorityRed, PriorityGreen, PriorityOrange, PriorityGrey)
                        angles.forEachIndexed { index, sweepAngle ->
                            drawArc(
                                color = colors[index],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Hôm nay",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatDuration(totalSeconds).substringBeforeLast(":"),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = JetBrainsMonoFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bullets to the right
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (q in 1..4) {
                        val qColor = when (q) {
                            1 -> PriorityRed
                            2 -> PriorityGreen
                            3 -> PriorityOrange
                            else -> PriorityGrey
                        }
                        val qName = when (q) {
                            1 -> "Khẩn cấp"
                            2 -> "Quan trọng"
                            3 -> "Ủy thác"
                            else -> "Thư giãn"
                        }

                        val qSec = logs.filter { it.isSynced == false }.sumOf { log ->
                            val task = tasks.firstOrNull { it.id == log.taskId }
                            if (task?.quadrant == q && log.endTime != null) log.durationSeconds else 0L
                        }
                        val taskAccumulated = tasks.filter { it.quadrant == q }.sumOf { it.actualMinutes * 60L }
                        val qTotal = (qSec + taskAccumulated).coerceAtLeast(0L)
                        val percent = if (totalSeconds > 0) (qTotal * 100 / totalSeconds).toInt() else 0

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(qColor)
                            )
                            Column {
                                Text(
                                    text = "$q. $qName",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatDuration(qTotal)}  ($percent%)",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSubPage(tasks: List<com.example.core.database.TaskEntity>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Danh sách chi tiết nhiệm vụ",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (tasks.isEmpty()) {
            Text(
                text = "Hôm nay chưa có dữ liệu công việc để hiển thị chi tiết.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
        }

        tasks.forEach { task ->
            val qColor = when (task.quadrant) {
                1 -> PriorityRed
                2 -> PriorityGreen
                3 -> PriorityOrange
                else -> PriorityGrey
            }
            val statusIcon = if (task.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Status",
                    tint = if (task.status == "COMPLETED") PriorityGreen else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quadrant ${task.quadrant} • ${task.estimatedMinutes}m dự kiến",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

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
                        text = "${task.actualMinutes} ph",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = JetBrainsMonoFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineSubPage(tasks: List<com.example.core.database.TaskEntity>, logs: List<TimeLogEntity>) {
    Column {
        Text(
            text = "Dòng thời gian hoạt động",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (logs.isEmpty()) {
            Text(
                text = "Hôm nay chưa bắt đầu đếm giờ nhiệm vụ nào để ghi nhận trên Timeline.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
        }

        logs.forEachIndexed { index, log ->
            val task = tasks.firstOrNull { it.id == log.taskId }
            val qColor = when (task?.quadrant) {
                1 -> PriorityRed
                2 -> PriorityGreen
                3 -> PriorityOrange
                else -> PriorityGrey
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Line timeline indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(qColor)
                    )
                    if (index < logs.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(56.dp)
                                .background(Color.Gray.copy(alpha = 0.3f))
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = task?.title ?: "Công việc không tên",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Thời lượng: ${log.durationSeconds / 60} phút ${log.durationSeconds % 60} giây",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AiAnalysisSubPage(viewModel: TimeTrackerViewModel) {
    val aiResponse by viewModel.aiAnalysisText.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (aiResponse.isEmpty() && !aiLoading) {
            viewModel.generateAiAnalysis()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Review Card standard
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LightSurface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = "Robot AI",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nhận xét của AI",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (aiLoading) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(24.dp).padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Đang triệu gọi trí tuệ nhân tạo phân tích chuyên sâu hiệu năng của bạn...",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    } else if (aiResponse.isEmpty()) {
                        Text(
                            text = "Bấm nút bên dưới để yêu cầu mô hình Gemini AI phân tích thói quen phân bổ thời gian của bạn ngày hôm nay.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = aiResponse,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.generateAiAnalysis() },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = !aiLoading
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Action")
                Text(text = if (aiLoading) "Đang phân tích..." else "Phân tích ngay với Gemini", fontSize = 14.sp)
            }
        }

        // Trends section (Screenshot 7 details setup)
        Text(
            text = "Xu hướng của bạn",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )

        TrendItem(
            iconColor = PriorityGreen,
            label = "Giờ làm việc hiệu quả nhất: 09:00 - 11:00",
            icon = Icons.Default.CheckCircle
        )
        TrendItem(
            iconColor = PriorityRed,
            label = "Giờ dễ bị phân tâm nhất: 15:00 - 16:00",
            icon = Icons.Default.Cancel
        )
        TrendItem(
            iconColor = PriorityOrange,
            label = "Bạn thường làm việc nhiều nhất vào: Thứ 3, Thứ 5",
            icon = Icons.Default.EventNote
        )

        // Suggestion checklist for tomorrow
        Text(
            text = "Đề xuất cho ngày mai",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )

        SuggestionCheckItem(label = "Ưu tiên hoàn thành: 3 task quan trọng")
        SuggestionCheckItem(label = "Hạn chế kiểm tra email sau 16:00")
        SuggestionCheckItem(label = "Dành thời gian cho việc lập kế hoạch buổi sáng")
    }
}

@Composable
fun TrendItem(iconColor: Color, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Trend Icon",
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SuggestionCheckItem(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Check Icon",
            tint = PriorityGreen,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}
