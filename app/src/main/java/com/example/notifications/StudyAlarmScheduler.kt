package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar

object StudyAlarmScheduler {

    private const val REQUEST_CODE_STUDY = 2001
    private const val REQUEST_CODE_EXAM = 2002

    fun scheduleDailyReminder(context: Context, hour: Int = 9, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = StudyReminderReceiver.ACTION_DAILY_STUDY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_STUDY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = StudyReminderReceiver.ACTION_DAILY_STUDY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_STUDY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun triggerInstantStudyReminder(context: Context) {
        NotificationHelper.createNotificationChannels(context)
        NotificationHelper.showStudyReminder(
            context = context,
            title = "Study Session Reminder 📚",
            message = "Time for your scheduled study session! Don't break your consistency streak."
        )
    }

    fun triggerInstantExamReminder(context: Context) {
        NotificationHelper.createNotificationChannels(context)
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
                } else {
                    NotificationHelper.showExamAlert(
                        context = context,
                        examSubject = "Computer Networks",
                        daysRemaining = 21,
                        examDate = "2026-12-02"
                    )
                }
            } catch (e: Exception) {
                NotificationHelper.showExamAlert(
                    context = context,
                    examSubject = "Computer Networks",
                    daysRemaining = 21,
                    examDate = "2026-12-02"
                )
            }
        }
    }
}
