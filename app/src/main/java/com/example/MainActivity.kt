package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.NoteEntity
import com.example.core.database.TaskEntity
import com.example.ui.TimeTrackerViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: TimeTrackerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreenIndex by remember { mutableStateOf(0) } // 0=Home, 1=Logs, 2=Report, 3=Settings
                var showAddSheet by remember { mutableStateOf(false) }

                // Editing targets
                var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
                var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        if (editingTask == null && editingNote == null) {
                            BottomNavigationBar(
                                currentIndex = currentScreenIndex,
                                onIndexChange = { currentScreenIndex = it },
                                onAddClick = { showAddSheet = true }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (editingTask == null && editingNote == null) innerPadding.calculateBottomPadding() else 0.dp)
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        // Display correct screen based on main state index or overlay editing sub-screen
                        when {
                            editingTask != null -> {
                                EditTaskSubScreen(
                                    task = editingTask!!,
                                    viewModel = viewModel,
                                    onDismiss = { editingTask = null }
                                )
                            }
                            editingNote != null -> {
                                EditNoteSubScreen(
                                    note = editingNote!!,
                                    viewModel = viewModel,
                                    onDismiss = { editingNote = null }
                                )
                            }
                            else -> {
                                when (currentScreenIndex) {
                                    0 -> HomeScreen(
                                        viewModel = viewModel,
                                        onQuadrantClick = { quadrant ->
                                            // Prefill quadrant in bottom sheet and open it!
                                            showAddSheet = true
                                        }
                                    )
                                    1 -> LogsScreen(
                                        viewModel = viewModel,
                                        onEditTask = { task -> editingTask = task },
                                        onEditNote = { note -> editingNote = note }
                                    )
                                    2 -> ReportScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreenIndex = 0 }
                                    )
                                    else -> SettingsScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreenIndex = 0 }
                                    )
                                }
                            }
                        }

                        // Bottom sheet logic overlay for creating tasks or notes
                        if (showAddSheet) {
                            AddTaskOrNoteBottomSheet(
                                viewModel = viewModel,
                                onDismiss = { showAddSheet = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home bottom navigation
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Trang chủ",
                isSelected = currentIndex == 0,
                onClick = { onIndexChange(0) },
                modifier = Modifier.weight(1f)
            )

            // Logs / List bottom navigation
            BottomNavItem(
                icon = Icons.Default.List,
                label = "Danh sách",
                isSelected = currentIndex == 1,
                onClick = { onIndexChange(1) },
                modifier = Modifier.weight(1f)
            )

            // Oversized Floating action button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = (-20).dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm mới",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Reports / Stats bottom navigation
            BottomNavItem(
                icon = Icons.Default.Assignment,
                label = "Báo cáo",
                isSelected = currentIndex == 2,
                onClick = { onIndexChange(2) },
                modifier = Modifier.weight(1f)
            )

            // Settings bottom navigation
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Cài đặt",
                isSelected = currentIndex == 3,
                onClick = { onIndexChange(3) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryBlue else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) PrimaryBlue else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskOrNoteBottomSheet(
    viewModel: TimeTrackerViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Công việc") } // "Công việc" or "Ghi chú"
    var title by remember { mutableStateOf("") }
    var contentDesc by remember { mutableStateOf("") }
    var estimatedMins by remember { mutableStateOf("30") }
    var startImmediately by remember { mutableStateOf(false) }

    // Selected Eisenhower priority quadrant
    var selectedQuadrant by remember { mutableStateOf(1) } // 1 to 4

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet title header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thêm công việc mới",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Tab selector index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf("Công việc", "Ghi chú")
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryBlue else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // INPUT TITLE
            Text(
                text = if (selectedTab == "Công việc") "TÊN CÔNG VIỆC" else "TIÊU ĐỀ GHI CHÚ",
                color = FloatColorDarkWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Bạn đang làm gì?", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Description or Content field
            Text(
                text = if (selectedTab == "Công việc") "MÔ TẢ CHI TIẾT" else "NỘI DUNG GHI CHÚ",
                color = FloatColorDarkWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = contentDesc,
                onValueChange = { contentDesc = it },
                placeholder = { Text("Mô tả cụ thể hoạt động...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            // SELECT PRIORITY QUADRANT CARDS
            Text(
                text = "MỨC ĐỘ ƯU TIÊN (EISENHOWER)",
                color = FloatColorDarkWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            GridQuadrantOptions(
                selectedQuadrant = selectedQuadrant,
                onQuadrantSelect = { selectedQuadrant = it }
            )

            if (selectedTab == "Công việc") {
                // Estimated time input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Thời gian dự kiến (phút)", color = Color.White, fontSize = 14.sp)
                    OutlinedTextField(
                        value = estimatedMins,
                        onValueChange = { estimatedMins = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.width(80.dp)
                    )
                }

                // Immediate timer start switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer Icon", tint = PrimaryBlue)
                        Text(text = "Bắt đầu đếm giờ ngay", color = Color.White, fontSize = 14.sp)
                    }

                    Switch(
                        checked = startImmediately,
                        onCheckedChange = { startImmediately = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryBlue,
                            checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm trigger buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Hủy", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (title.isNotEmpty()) {
                            if (selectedTab == "Công việc") {
                                val estMinutes = estimatedMins.toIntOrNull() ?: 30
                                viewModel.createTask(
                                    title = title,
                                    description = contentDesc,
                                    quadrant = selectedQuadrant,
                                    estimatedMinutes = estMinutes,
                                    startImmediately = startImmediately
                                )
                            } else {
                                viewModel.createNote(
                                    title = title,
                                    content = contentDesc,
                                    quadrant = selectedQuadrant
                                )
                            }
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp)
                ) {
                    Text(
                        text = if (startImmediately) "Thêm & Bắt đầu" else "Thêm mới",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GridQuadrantOptions(
    selectedQuadrant: Int,
    onQuadrantSelect: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuadrantOptionCard(
                qNum = 1,
                label = "Quan trọng - Khẩn cấp",
                isSelected = selectedQuadrant == 1,
                qColor = PriorityRed,
                onClick = { onQuadrantSelect(1) },
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
            QuadrantOptionCard(
                qNum = 2,
                label = "Quan trọng - Không khẩn cấp",
                isSelected = selectedQuadrant == 2,
                qColor = PriorityGreen,
                onClick = { onQuadrantSelect(2) },
                icon = Icons.Default.CalendarToday,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuadrantOptionCard(
                qNum = 3,
                label = "Không quan trọng - Khẩn cấp",
                isSelected = selectedQuadrant == 3,
                qColor = PriorityOrange,
                onClick = { onQuadrantSelect(3) },
                icon = Icons.Default.Notifications,
                modifier = Modifier.weight(1f)
            )
            QuadrantOptionCard(
                qNum = 4,
                label = "Không quan trọng - Không khẩn cấp",
                isSelected = selectedQuadrant == 4,
                qColor = PriorityGrey,
                onClick = { onQuadrantSelect(4) },
                icon = Icons.Default.Layers,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuadrantOptionCard(
    qNum: Int,
    label: String,
    isSelected: Boolean,
    qColor: Color,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = if (isSelected) qColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = qColor,
                    modifier = Modifier.size(20.dp)
                )

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = qColor,
                        unselectedColor = Color.Gray
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp
            )
        }
    }
}

// --- SUB SCREEN FOR EDITING SESSION (TASK LOGS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskSubScreen(
    task: TaskEntity,
    viewModel: TimeTrackerViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var durationMinutes by remember { mutableStateOf(task.actualMinutes.toString()) }
    var selectedQuadrant by remember { mutableStateOf(task.quadrant) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Trở lại", tint = Color.White)
                }
                Text(
                    text = "EDIT SESSION",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "SESSION SUMMARY",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Title and notes
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Tên công việc") },
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Ghi chú chi tiết") },
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 16.dp)
        )

        Text(
            text = "DURATION (MIN)",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = durationMinutes,
            onValueChange = { durationMinutes = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Text(
            text = "EISENHOWER PRIORITY",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GridQuadrantOptions(
            selectedQuadrant = selectedQuadrant,
            onQuadrantSelect = { selectedQuadrant = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        description = description,
                        actualMinutes = durationMinutes.toIntOrNull() ?: task.actualMinutes,
                        quadrant = selectedQuadrant,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "SAVE CHANGES", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.deleteTask(task)
                onDismiss()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PriorityRed),
            border = BorderStroke(1.dp, PriorityRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "DELETE LOG", fontWeight = FontWeight.Bold)
        }
    }
}

// --- SUB SCREEN FOR EDITING GHI CHÚ (NOTES) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteSubScreen(
    note: NoteEntity,
    viewModel: TimeTrackerViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var selectedQuadrant by remember { mutableStateOf(note.quadrant) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Trở lại", tint = Color.White)
                }
                Text(
                    text = "EDIT NOTE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "NOTE HEADER",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Tiêu đề ghi chú") },
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Nội dung ghi chú") },
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 16.dp)
        )

        Text(
            text = "ASSOCIATED PRIORITY QUADRANT",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GridQuadrantOptions(
            selectedQuadrant = selectedQuadrant,
            onQuadrantSelect = { selectedQuadrant = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.updateNote(
                    note.copy(
                        title = title,
                        content = content,
                        quadrant = selectedQuadrant
                    )
                )
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Save Note")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "SAVE CHANGES", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.deleteNote(note)
                onDismiss()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PriorityRed),
            border = BorderStroke(1.dp, PriorityRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Note")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "DELETE NOTE", fontWeight = FontWeight.Bold)
        }
    }
}

val FloatColorDarkWhite = Color(0xFFB0BEC5)
