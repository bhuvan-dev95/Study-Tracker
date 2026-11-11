package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyConstants
import com.example.ui.components.CategoryBadge
import com.example.ui.components.StatCard
import com.example.ui.components.getCategoryColor
import com.example.ui.viewmodel.StudyViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: StudyViewModel
) {
    val tasks by viewModel.allTasks.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()

    val today = remember { LocalDate.now() }
    val todayStr = remember { viewModel.getTodayDateString() }

    // 1. Daily Study Hours
    val todayMinutes = sessions.filter { it.dateString == todayStr }.sumOf { it.durationMinutes }
    val todayHoursFormatted = String.format(Locale.US, "%.1f h", todayMinutes / 60.0)

    // 2. Weekly Study Hours (Last 7 days)
    val sevenDaysAgo = remember { today.minusDays(6) }
    val weeklyMinutes = sessions.filter {
        try {
            val d = LocalDate.parse(it.dateString)
            !d.isBefore(sevenDaysAgo) && !d.isAfter(today)
        } catch (e: Exception) {
            false
        }
    }.sumOf { it.durationMinutes }
    val weeklyHoursFormatted = String.format(Locale.US, "%.1f h", weeklyMinutes / 60.0)

    // 3. Monthly Study Hours (Last 30 days)
    val thirtyDaysAgo = remember { today.minusDays(29) }
    val monthlyMinutes = sessions.filter {
        try {
            val d = LocalDate.parse(it.dateString)
            !d.isBefore(thirtyDaysAgo) && !d.isAfter(today)
        } catch (e: Exception) {
            false
        }
    }.sumOf { it.durationMinutes }
    val monthlyHoursFormatted = String.format(Locale.US, "%.1f h", monthlyMinutes / 60.0)

    // 4. Task Completion Rate
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val completionRateFraction = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
    val completionPercentage = (completionRateFraction * 100).toInt()

    // 5. Past 7 Days Weekly Bar Chart Data
    val past7DaysData = remember(sessions) {
        (6 downTo 0).map { dayOffset ->
            val date = today.minusDays(dayOffset.toLong())
            val dStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val label = date.format(DateTimeFormatter.ofPattern("EEE"))
            val dayMins = sessions.filter { it.dateString == dStr }.sumOf { it.durationMinutes }
            DayChartData(
                dayLabel = label,
                minutes = dayMins,
                hours = dayMins / 60.0f
            )
        }
    }
    val maxWeeklyHours = past7DaysData.maxOfOrNull { it.hours }?.coerceAtLeast(2.0f) ?: 2.0f

    // 6. Category-wise Distribution
    val totalAllSessionMinutes = sessions.sumOf { it.durationMinutes }.coerceAtLeast(1)
    val categoryStats = StudyConstants.CATEGORIES.map { cat ->
        val catMins = sessions.filter { it.category == cat }.sumOf { it.durationMinutes }
        val percentage = (catMins.toFloat() / totalAllSessionMinutes.toFloat() * 100).toInt()
        CategoryProgressData(
            category = cat,
            minutes = catMins,
            percentage = percentage
        )
    }

    // 7. Subject-wise breakdown (sorted by most studied)
    val subjectStats = sessions
        .groupBy { it.subject }
        .map { (subject, sList) ->
            val category = sList.firstOrNull()?.category ?: StudyConstants.CAT_BE_COMP
            val totalMins = sList.sumOf { it.durationMinutes }
            SubjectStatData(
                subject = subject,
                category = category,
                minutes = totalMins,
                hoursFormatted = String.format(Locale.US, "%.1f hrs", totalMins / 60.0)
            )
        }
        .sortedByDescending { it.minutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "Study Progress Analytics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Real-time stats computed from logged study sessions and completed tasks",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Today",
                    value = todayHoursFormatted,
                    subtitle = "study time",
                    icon = Icons.Default.Schedule,
                    iconTint = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "This Week",
                    value = weeklyHoursFormatted,
                    subtitle = "past 7 days",
                    icon = Icons.Default.DateRange,
                    iconTint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "This Month",
                    value = monthlyHoursFormatted,
                    subtitle = "past 30 days",
                    icon = Icons.Default.Assessment,
                    iconTint = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Progress Bar Chart (Last 7 Days)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Study Hours",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Past 7 Days",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Visual Custom Bar Chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        past7DaysData.forEach { data ->
                            val heightFraction = (data.hours / maxWeeklyHours).coerceIn(0.08f, 1f)
                            val animatedHeight by animateFloatAsState(targetValue = heightFraction, label = "bar")

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (data.hours > 0) {
                                    Text(
                                        text = String.format(Locale.US, "%.1fh", data.hours),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(fraction = animatedHeight)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (data.hours > 0)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = data.dayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Task Completion Percentage Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(76.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            strokeWidth = 8.dp
                        )
                        CircularProgressIndicator(
                            progress = { completionRateFraction },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF10B981),
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "$completionPercentage%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Task Completion Rate",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$completedTasks of $totalTasks tasks completed",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${totalTasks - completedTasks} pending university & exam tasks",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Category-wise Time Distribution
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category Time Distribution",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    categoryStats.forEach { stat ->
                        val catColor = getCategoryColor(stat.category)
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stat.category,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "${stat.minutes}m (${stat.percentage}%)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = catColor,
                                trackColor = catColor.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }

        // Subject-wise Study Time
        item {
            Text(
                text = "Subject-wise Study Time Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (subjectStats.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No study sessions recorded yet. Start a Pomodoro timer to build your statistics!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(subjectStats) { subStat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subStat.subject,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            CategoryBadge(category = subStat.category)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = subStat.hoursFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

data class DayChartData(
    val dayLabel: String,
    val minutes: Int,
    val hours: Float
)

data class CategoryProgressData(
    val category: String,
    val minutes: Int,
    val percentage: Int
)

data class SubjectStatData(
    val subject: String,
    val category: String,
    val minutes: Int,
    val hoursFormatted: String
)
