package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.NoteEntity
import com.example.core.database.TaskEntity
import com.example.ui.TimeTrackerViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: TimeTrackerViewModel,
    onEditTask: (TaskEntity) -> Unit,
    onEditNote: (NoteEntity) -> Unit
) {
    val tasks by viewModel.allTasksFlow.collectAsState(initial = emptyList())
    val notes by viewModel.allNotesFlow.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterQuadrant by remember { mutableStateOf<Int?>(null) } // null = All

    val filteredItems = remember(tasks, notes, searchQuery, selectedFilterQuadrant) {
        val mappedTasks = tasks.map { LogOrNoteItem.Task(it) }
        val mappedNotes = notes.map { LogOrNoteItem.Note(it) }
        val combined = (mappedTasks + mappedNotes).sortedByDescending { it.timestamp }

        combined.filter { item ->
            val matchesQuery = when (item) {
                is LogOrNoteItem.Task -> item.task.title.contains(searchQuery, ignoreCase = true) || item.task.description.contains(searchQuery, ignoreCase = true)
                is LogOrNoteItem.Note -> item.note.title.contains(searchQuery, ignoreCase = true) || item.note.content.contains(searchQuery, ignoreCase = true)
            }
            val matchesQuadrant = selectedFilterQuadrant == null || when (item) {
                is LogOrNoteItem.Task -> item.task.quadrant == selectedFilterQuadrant
                is LogOrNoteItem.Note -> item.note.quadrant == selectedFilterQuadrant
            }
            matchesQuery && matchesQuadrant
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // --- Header focus ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "FOCUS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- Search bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search sessions or notes...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        // --- Priority Filter Chips Row ---
        ScrollableTabRow(
            selectedTabIndex = when (selectedFilterQuadrant) {
                null -> 0
                else -> selectedFilterQuadrant!!
            },
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent,
            indicator = {},
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            val filters = listOf(
                FilterData(null, "All"),
                FilterData(1, "Important/Urgent"),
                FilterData(2, "Important/Not Urgent"),
                FilterData(3, "Urgent/Not Important"),
                FilterData(4, "Not Urgent/Important")
            )

            filters.forEach { filter ->
                val isSelected = selectedFilterQuadrant == filter.quadrant
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterQuadrant = filter.quadrant },
                    label = { Text(filter.label, color = if (isSelected) Color.White else OnSurfaceNeutral, fontSize = 12.sp) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        containerColor = if (isSelected) PrimaryBlue else LightSurface,
                        selectedContainerColor = PrimaryBlue
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Grouped Logs LazyColumn ---
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không tìm thấy phiên làm việc hay ghi chú nào.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Group our combined filtered lists by formatted Date Header
            val groupedByDate = filteredItems.groupBy { item ->
                formatDateGroup(item.timestamp)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedByDate.forEach { (dateHeader, itemsInGroup) ->
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateHeader.uppercase(),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    items(itemsInGroup) { item ->
                        when (item) {
                            is LogOrNoteItem.Task -> {
                                TaskLogCard(
                                    task = item.task,
                                    onClick = { onEditTask(item.task) }
                                )
                            }
                            is LogOrNoteItem.Note -> {
                                NoteLogCard(
                                    note = item.note,
                                    onClick = { onEditNote(item.note) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateGroup(timestamp: Long): String {
    val dayFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(timestamp))
    val today = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
    val diff = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
    return when {
        dayFormat == today -> "TODAY, " + SimpleDateFormat("MMM dd", Locale.US).format(Date(timestamp))
        diff == 1L -> "YESTERDAY, " + SimpleDateFormat("MMM dd", Locale.US).format(Date(timestamp))
        else -> dayFormat
    }
}

@Composable
fun TaskLogCard(task: TaskEntity, onClick: () -> Unit) {
    val qColor = when (task.quadrant) {
        1 -> PriorityRed
        2 -> PriorityGreen
        3 -> PriorityOrange
        else -> PriorityGrey
    }
    val qName = when (task.quadrant) {
        1 -> "Do First"
        2 -> "Schedule"
        3 -> "Delegate"
        else -> "Eliminate"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, qColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    color = qColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${task.actualMinutes}m",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontFamily = JetBrainsMonoFont,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Created At",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = SimpleDateFormat("hh:mm a", Locale.US).format(Date(task.createdAt)),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    // Quadrant Label Badge
                    Text(
                        text = "▲ $qName",
                        color = qColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NoteLogCard(note: NoteEntity, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Note Icon",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Note",
                        color = PrimaryBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (note.content.isNotEmpty()) {
                    Text(
                        text = note.content,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Created",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = SimpleDateFormat("hh:mm a", Locale.US).format(Date(note.createdAt)),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// Helper models
data class FilterData(val quadrant: Int?, val label: String)

sealed class LogOrNoteItem {
    abstract val timestamp: Long

    data class Task(val task: TaskEntity) : LogOrNoteItem() {
        override val timestamp: Long get() = task.createdAt
    }

    data class Note(val note: NoteEntity) : LogOrNoteItem() {
        override val timestamp: Long get() = note.createdAt
    }
}
