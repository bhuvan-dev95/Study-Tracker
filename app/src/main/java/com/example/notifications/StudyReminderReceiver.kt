package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class StudyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_STUDY_REMINDER = "com.example.action.DAILY_STUDY_REMINDER"
        const val ACTION_EXAM_COUNTDOWN_REMINDER = "com.example.action.EXAM_COUNTDOWN_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.createNotificationChannels(context)

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule reminders if user had enabled them
                val prefs = context.getSharedPreferences("study_tracker_prefs", Context.MODE_PRIVATE)
                val remindersEnabled = prefs.getBoolean("reminders_enabled", true)
                if (remindersEnabled) {
                    val hour = prefs.getInt("reminder_hour", 9)
                    val minute = prefs.getInt("reminder_minute", 0)
                    StudyAlarmScheduler.scheduleDailyReminder(context, hour, minute)
                }
            }

            ACTION_DAILY_STUDY_REMINDER -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val todayTasks = db.taskDao().getAllTasksSnapshot().filter { it.dateString == todayStr }
                        val pendingCount = todayTasks.count { !it.isCompleted }

                        val message = if (pendingCount > 0) {
                            "You have $pendingCount pending study task(s) scheduled for today. Start a Pomodoro session to stay on track!"
                        } else {
                            "Time for your daily study! Review your revision topics and prepare for your upcoming university exams."
                        }

                        NotificationHelper.showStudyReminder(
                            context = context,
                            title = "Study Session Reminder 📚",
                            message = message
                        )
                    } catch (e: Exception) {
                        NotificationHelper.showStudyReminder(
                            context = context,
                            title = "Daily Study Reminder 📚",
                            message = "Consistency is key to cracking BE exams, GPSC, and SSC CGL. Time to study!"
                        )
                    }
                }
            }

            ACTION_EXAM_COUNTDOWN_REMINDER -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val exams = db.examDao().getAllExamsSnapshot()
                        val today = LocalDate.now()

                        val upcoming = exams.mapNotNull { exam ->
                            try {
                                val d = LocalDate.parse(exam.examDate)
                                val days = ChronoUnit.DAYS.between(today, d)
                                if (days >= 0) exam to days else null
                            } catch (e: Exception) {
                                null
                            }
                        }.minByOrNull { it.second }

                        if (upcoming != null) {
                            val (exam, daysRemaining) = upcoming
                            NotificationHelper.showExamAlert(
                                context = context,
                                examSubject = exam.subject,
                                daysRemaining = daysRemaining,
                                examDate = exam.examDate
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
