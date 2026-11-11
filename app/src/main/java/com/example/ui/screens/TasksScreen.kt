package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyConstants
import com.example.data.model.StudyTask
import com.example.data.model.TaskPriority
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.StudyViewModel

enum class TaskTab(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: StudyViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    var selectedTab by remember { mutableStateOf(TaskTab.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedPriorityFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog & BottomSheet state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTask?>(null) }
    var taskToDelete by remember { mutableStateOf<StudyTask?>(null) }
    var taskToLogActualTime by remember { mutableStateOf<StudyTask?>(null) }

    // Filter tasks
    val filteredTasks = tasks.filter { task ->
        val matchesTab = when (selectedTab) {
            TaskTab.ALL -> true
            TaskTab.PENDING -> !task.isCompleted
            TaskTab.COMPLETED -> task.isCompleted
        }
        val matchesCategory = selectedCategoryFilter == null || task.category == selectedCategoryFilter
        val matchesPriority = selectedPriorityFilter == null || task.priority == selectedPriorityFilter
        val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.subject.contains(searchQuery, ignoreCase = true) ||
                task.notes.contains(searchQuery, ignoreCase = true)

        matchesTab && matchesCategory && matchesPriority && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("tasks_screen")
        ) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Task Planner",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${filteredTasks.size} tasks shown",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by title, subject, notes...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_tasks_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs: All, Pending, Completed
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                TaskTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Category & Priority filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All Categories") }
                    )
                }
                items(StudyConstants.CATEGORIES) { cat ->
                    val shortName = when {
                        cat.contains("BE") -> "BE Comp"
                        cat.contains("GPSC") -> "GPSC"
                        cat.contains("SSC") -> "SSC CGL"
                        else -> cat
                    }
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        label = { Text(shortName) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPriorityFilter == TaskPriority.HIGH.name,
                        onClick = {
                            selectedPriorityFilter = if (selectedPriorityFilter == TaskPriority.HIGH.name) null else TaskPriority.HIGH.name
                        },
                        label = { Text("High Priority") }
                    )
                }
            }

            // Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No study tasks found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below to plan a task for your exams.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                            onEdit = {
                                taskToEdit = task
                                showAddEditDialog = true
                            },
                            onDelete = {
                                taskToDelete = task
                            },
                            onLogTime = {
                                taskToLogActualTime = task
                            },
                            onStartTimer = {
                                viewModel.setTimerCategory(task.category)
                                viewModel.setTimerSubject(task.subject)
                                viewModel.setTimerSelectedTaskId(task.id)
                                onNavigate(AppScreen.TIMER)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Task Dialog
    if (showAddEditDialog) {
        AddEditTaskDialog(
            initialTask = taskToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { title, cat, sub, priority, plannedMins, dateStr, notes ->
                if (taskToEdit == null) {
                    viewModel.addTask(
                        title = title,
                        category = cat,
                        subject = sub,
                        priority = priority,
                        plannedMinutes = plannedMins,
                        dateString = dateStr,
                        notes = notes
                    )
                } else {
                    val updated = taskToEdit!!.copy(
                        title = title,
                        category = cat,
                        subject = sub,
                        priority = priority.name,
                        plannedMinutes = plannedMins,
                        dateString = dateStr,
                        notes = notes
                    )
                    viewModel.updateTask(updated)
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    taskToDelete?.let { task ->
        ConfirmDeleteDialog(
            title = "Delete Task?",
            message = "Are you sure you want to delete '${task.title}'? This cannot be undone.",
            onConfirm = {
                viewModel.deleteTask(task)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    // Log Actual Duration Dialog
    taskToLogActualTime?.let { task ->
        var actualMinsInput by remember { mutableStateOf(task.actualMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { taskToLogActualTime = null },
            title = { Text("Log Actual Study Time") },
            text = {
                Column {
                    Text(
                        text = "Task: ${task.title}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Planned duration was ${task.plannedMinutes} minutes.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = actualMinsInput,
                        onValueChange = { actualMinsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Actual Minutes Studied") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = actualMinsInput.toIntOrNull() ?: task.actualMinutes
                        viewModel.updateTask(task.copy(actualMinutes = mins))
                        taskToLogActualTime = null
                    }
                ) {
                    Text("Save Time")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToLogActualTime = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TaskCard(
    task: StudyTask,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogTime: () -> Unit,
    onStartTimer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.testTag("task_check_${task.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = task.subject,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Options Menu
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.notes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 44.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Badges & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(category = task.category)
                    PriorityBadge(priority = task.priority)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Duration badge (clickable to log actual time)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onLogTime() }
                    ) {
                        Text(
                            text = "${task.actualMinutes}m / ${task.plannedMinutes}m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (!task.isCompleted) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onStartTimer() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Pomodoro",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Timer",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
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
fun AddEditTaskDialog(
    initialTask: StudyTask?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        category: String,
        subject: String,
        priority: TaskPriority,
        plannedMinutes: Int,
        dateString: String,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTask?.category ?: StudyConstants.CAT_BE_COMP) }
    var selectedSubject by remember {
        mutableStateOf(
            initialTask?.subject ?: (StudyConstants.SUBJECTS_BY_CATEGORY[StudyConstants.CAT_BE_COMP]?.first() ?: "Computer Networks")
        )
    }
    var selectedPriority by remember {
        mutableStateOf(
            if (initialTask != null) TaskPriority.valueOf(initialTask.priority) else TaskPriority.MEDIUM
        )
    }
    var plannedMinutesInput by remember { mutableStateOf((initialTask?.plannedMinutes ?: 60).toString()) }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var titleError by remember { mutableStateOf(false) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    val subjects = StudyConstants.SUBJECTS_BY_CATEGORY[selectedCategory] ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask == null) "Add Daily Study Task" else "Edit Task",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Task Title *") },
                    isError = titleError,
                    supportingText = {
                        if (titleError) {
                            Text("Title cannot be empty")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Study Category") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        StudyConstants.CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                    // Update subject to first of this category
                                    val newSubs = StudyConstants.SUBJECTS_BY_CATEGORY[cat] ?: emptyList()
                                    if (newSubs.isNotEmpty()) {
                                        selectedSubject = newSubs.first()
                                    }
                                }
                            )
                        }
                    }
                }

                // Subject Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subjectDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    selectedSubject = sub
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority Selection
                Column {
                    Text(
                        text = "Priority Level:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.values().forEach { priority ->
                            FilterChip(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority },
                                label = { Text(priority.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Planned duration
                OutlinedTextField(
                    value = plannedMinutesInput,
                    onValueChange = { plannedMinutesInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Planned Study Duration (Minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Topics to cover") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val planned = plannedMinutesInput.toIntOrNull() ?: 60
                    onSave(
                        title.trim(),
                        selectedCategory,
                        selectedSubject,
                        selectedPriority,
                        planned,
                        initialTask?.dateString ?: java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        notes.trim()
                    )
                },
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
