package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_STUDY_REMINDERS = "channel_study_reminders"
    const val CHANNEL_EXAM_ALERTS = "channel_exam_alerts"
    const val CHANNEL_TIMER_ALERTS = "channel_timer_alerts"

    const val NOTIFICATION_ID_STUDY = 1001
    const val NOTIFICATION_ID_EXAM = 1002
    const val NOTIFICATION_ID_TIMER = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Channel 1: Daily Study Reminders
            val studyChannel = NotificationChannel(
                CHANNEL_STUDY_REMINDERS,
                "Daily Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to maintain consistency and fulfill study goals"
                enableVibration(true)
            }

            // Channel 2: Upcoming Exam Alerts
            val examChannel = NotificationChannel(
                CHANNEL_EXAM_ALERTS,
                "Upcoming Exam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority countdown and preparation alerts for upcoming exams"
                enableVibration(true)
            }

            // Channel 3: Pomodoro Timer Alerts
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ALERTS,
                "Pomodoro Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your Pomodoro study session or break finishes"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(studyChannel, examChannel, timerChannel)
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        // Tap action: open MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showStudyReminder(context: Context, title: String, message: String) {
        showNotification(
            context = context,
            channelId = CHANNEL_STUDY_REMINDERS,
            notificationId = NOTIFICATION_ID_STUDY,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showExamAlert(context: Context, examSubject: String, daysRemaining: Long, examDate: String) {
        val title = when {
            daysRemaining == 0L -> "Exam Today: $examSubject!"
            daysRemaining == 1L -> "Exam Tomorrow: $examSubject!"
            daysRemaining < 7L -> "Only $daysRemaining days left: $examSubject"
            else -> "Upcoming Exam: $examSubject in $daysRemaining days"
        }
        val message = "Your university exam is scheduled for $examDate. Stay focused and complete your planned revision topics today!"
        showNotification(
            context = context,
            channelId = CHANNEL_EXAM_ALERTS,
            notificationId = NOTIFICATION_ID_EXAM,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showTimerAlert(context: Context, isStudySession: Boolean, durationMinutes: Int) {
        val title = if (isStudySession) "Pomodoro Session Completed! 🎯" else "Break Finished! 🔔"
        val message = if (isStudySession) {
            "Great job! You logged $durationMinutes minutes of focused study. Take a well-deserved short break."
        } else {
            "Break time is over! Ready to dive back into your next study session?"
        }
        showNotification(
            context = context,
            channelId = CHANNEL_TIMER_ALERTS,
            notificationId = NOTIFICATION_ID_TIMER,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
}
