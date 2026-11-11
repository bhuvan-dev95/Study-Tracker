package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamItem
import com.example.data.model.StudyTask
import com.example.ui.components.CategoryBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatCard
import com.example.ui.components.getCategoryColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.StudyViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun DashboardScreen(
    viewModel: StudyViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val exams by viewModel.allExams.collectAsState()
    val dailyTargetMinutes by viewModel.dailyTargetMinutes.collectAsState()

    val today = remember { LocalDate.now() }
    val todayStr = remember { viewModel.getTodayDateString() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy") }
    val displayDate = remember { today.format(dateFormatter) }

    // Today's stats
    val todaySessions = sessions.filter { it.dateString == todayStr }
    val todayStudyMinutes = todaySessions.sumOf { it.durationMinutes }
    val todayStudyHoursStr = "${todayStudyMinutes / 60}h ${todayStudyMinutes % 60}m"

    val todayTasks = tasks.filter { it.dateString == todayStr }
    val completedTasksToday = todayTasks.count { it.isCompleted }
    val pendingTasksToday = todayTasks.count { !it.isCompleted }

    // Goal progress
    val targetMinutesSafe = if (dailyTargetMinutes > 0) dailyTargetMinutes else 120
    val progressFraction = (todayStudyMinutes.toFloat() / targetMinutesSafe.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    // Weekly study hours (last 7 days)
    val sevenDaysAgo = remember { today.minusDays(7) }
    val weeklyMinutes = sessions.filter {
        try {
            val sDate = LocalDate.parse(it.dateString)
            !sDate.isBefore(sevenDaysAgo) && !sDate.isAfter(today)
        } catch (e: Exception) {
            false
        }
    }.sumOf { it.durationMinutes }
    val weeklyHoursFormatted = String.format(java.util.Locale.US, "%.1f hrs", weeklyMinutes / 60.0)

    // Dynamic Streak Calculation
    val streakDays = remember(tasks, sessions) {
        var streak = 0
        var checkDate = today
        // Active dates are dates that have completed tasks or study sessions
        val activeDates = mutableSetOf<String>()
        tasks.filter { it.isCompleted }.forEach { activeDates.add(it.dateString) }
        sessions.filter { it.durationMinutes > 0 }.forEach { activeDates.add(it.dateString) }

        // If today has activity, count today and go backwards
        var curr = checkDate
        while (activeDates.contains(curr.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))) {
            streak++
            curr = curr.minusDays(1)
        }
        // If today has no activity yet, check yesterday to keep yesterday's streak alive
        if (streak == 0) {
            curr = checkDate.minusDays(1)
            while (activeDates.contains(curr.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))) {
                streak++
                curr = curr.minusDays(1)
            }
        }
        streak.coerceAtLeast(1) // Always give initial student streak
    }

    // Upcoming exams countdown
    val upcomingExams = exams.mapNotNull { exam ->
        try {
            val examLocalDate = LocalDate.parse(exam.examDate)
            val daysRemaining = ChronoUnit.DAYS.between(today, examLocalDate)
            ExamWithCountdown(exam, daysRemaining, isPast = daysRemaining < 0)
        } catch (e: Exception) {
            null
        }
    }.sortedBy { it.daysRemaining }

    // Dialog state for changing target
    var showTargetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Greeting & Date
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, Student 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = displayDate,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Streak Badge
                Surface(
                    color = Color(0xFFF97316).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streakDays Day Streak",
                            color = Color(0xFFF97316),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Daily Goal Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Study Target",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Target: ${targetMinutesSafe / 60}h ${targetMinutesSafe % 60}m",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { showTargetDialog = true },
                            modifier = Modifier.testTag("edit_target_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Target",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = todayStudyHoursStr,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}% completed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }

        // 3. Stat Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Completed",
                    value = "$completedTasksToday",
                    subtitle = "tasks done",
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.TASKS) }
                )
                StatCard(
                    title = "Pending",
                    value = "$pendingTasksToday",
                    subtitle = "tasks left",
                    icon = Icons.Default.PendingActions,
                    iconTint = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.TASKS) }
                )
                StatCard(
                    title = "Weekly",
                    value = weeklyHoursFormatted,
                    subtitle = "past 7 days",
                    icon = Icons.Default.DateRange,
                    iconTint = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.ANALYTICS) }
                )
            }
        }

        // 4. Quick Pomodoro Focus CTA Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AppScreen.TIMER) }
                    .testTag("start_pomodoro_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ready to Focus?",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Launch Pomodoro 25-min Study Timer",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Upcoming University Exams
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Upcoming Exams",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "View Schedule",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate(AppScreen.EXAMS) }
                )
            }
        }

        item {
            if (upcomingExams.isEmpty()) {
                Text(
                    text = "No exams scheduled.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(upcomingExams) { item ->
                        ExamCountdownCard(item = item)
                    }
                }
            }
        }

        // 6. Today's Planned Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Study Plan (${todayTasks.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                FilledTonalButton(
                    onClick = { onNavigate(AppScreen.TASKS) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", fontSize = 12.sp)
                }
            }
        }

        if (todayTasks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tasks planned for today",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { onNavigate(AppScreen.TASKS) }) {
                            Text("Plan Today's Tasks")
                        }
                    }
                }
            }
        } else {
            items(todayTasks) { task ->
                DashboardTaskItem(
                    task = task,
                    onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                    onStartTimer = {
                        viewModel.setTimerCategory(task.category)
                        viewModel.setTimerSubject(task.subject)
                        viewModel.setTimerSelectedTaskId(task.id)
                        onNavigate(AppScreen.TIMER)
                    }
                )
            }
        }

        // Bottom spacer for comfortable scrolling
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Edit Daily Target Dialog
    if (showTargetDialog) {
        var inputMinutes by remember { mutableStateOf((targetMinutesSafe).toString()) }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Set Daily Study Target") },
            text = {
                Column {
                    Text(
                        text = "Specify your target study duration in minutes (Default is 120 minutes = 2 hours):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputMinutes,
                        onValueChange = { inputMinutes = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutes per day") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_minutes_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val mins = inputMinutes.toIntOrNull() ?: 0
                    Text(
                        text = "Equivalent to: ${mins / 60} hours and ${mins % 60} minutes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = inputMinutes.toIntOrNull() ?: 120
                        if (mins > 0) {
                            viewModel.updateDailyTarget(mins)
                        }
                        showTargetDialog = false
                    },
                    modifier = Modifier.testTag("save_target_button")
                ) {
                    Text("Save Target")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTargetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class ExamWithCountdown(
    val exam: ExamItem,
    val daysRemaining: Long,
    val isPast: Boolean
)

@Composable
fun ExamCountdownCard(item: ExamWithCountdown) {
    val categoryColor = getCategoryColor(item.exam.category)

    val (badgeText, badgeColor) = when {
        item.isPast -> "Past Exam" to Color.Gray
        item.daysRemaining == 0L -> "TODAY" to Color(0xFFEF4444)
        item.daysRemaining == 1L -> "Tomorrow" to Color(0xFFEF4444)
        item.daysRemaining < 7L -> "${item.daysRemaining} days left" to Color(0xFFEF4444)
        item.daysRemaining < 14L -> "${item.daysRemaining} days left" to Color(0xFFF59E0B)
        else -> "${item.daysRemaining} days left" to Color(0xFF3B82F6)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(220.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(category = item.exam.category)

                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.exam.subject,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Exam: ${item.exam.examDate}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.exam.targetScore.isNotBlank()) {
                Text(
                    text = item.exam.targetScore,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DashboardTaskItem(
    task: StudyTask,
    onToggleComplete: () -> Unit,
    onStartTimer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryBadge(category = task.category)
                    PriorityBadge(priority = task.priority)
                    Text(
                        text = "${task.actualMinutes}/${task.plannedMinutes}m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!task.isCompleted) {
                IconButton(
                    onClick = onStartTimer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Timer for Task",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
