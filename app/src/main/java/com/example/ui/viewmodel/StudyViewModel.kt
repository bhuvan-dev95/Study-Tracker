package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ExamItem
import com.example.data.model.StudyConstants
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.TaskPriority
import com.example.data.model.TopicRevision
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

enum class AppScreen(val title: String) {
    DASHBOARD("Dashboard"),
    TASKS("Daily Tasks"),
    TIMER("Study Timer"),
    CALENDAR("Calendar"),
    ANALYTICS("Analytics"),
    REVISION("Notes & Revision"),
    EXAMS("Exams & Settings")
}

enum class TimerMode(val label: String, val defaultMinutes: Int) {
    STUDY_25("Study (25m)", 25),
    SHORT_BREAK("Short Break (5m)", 5),
    LONG_BREAK("Long Break (15m)", 15),
    CUSTOM("Custom", 45)
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED
}

class StudyViewModel(
    application: Application,
    private val repository: StudyRepository
) : AndroidViewModel(application) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }

    // Navigation Screen State
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    init {
        com.example.notifications.NotificationHelper.createNotificationChannels(application)
        if (repository.remindersEnabled.value) {
            com.example.notifications.StudyAlarmScheduler.scheduleDailyReminder(
                application,
                repository.reminderHour.value,
                repository.reminderMinute.value
            )
        }
    }

    // Repository Flows
    val allTasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExams = repository.allExams.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRevisions = repository.allRevisions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val dailyTargetMinutes = repository.dailyTargetMinutes
    val isDarkMode = repository.isDarkMode
    val remindersEnabled = repository.remindersEnabled
    val reminderHour = repository.reminderHour
    val reminderMinute = repository.reminderMinute
    val examAlertsEnabled = repository.examAlertsEnabled

    fun toggleDarkMode(enable: Boolean) {
        repository.setDarkMode(enable)
    }

    fun toggleReminders(enabled: Boolean) {
        repository.setRemindersEnabled(enabled)
        val app = getApplication<Application>()
        if (enabled) {
            com.example.notifications.StudyAlarmScheduler.scheduleDailyReminder(
                app,
                reminderHour.value,
                reminderMinute.value
            )
            viewModelScope.launch { _userMessage.emit("Study reminders enabled") }
        } else {
            com.example.notifications.StudyAlarmScheduler.cancelDailyReminder(app)
            viewModelScope.launch { _userMessage.emit("Study reminders disabled") }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        repository.setReminderTime(hour, minute)
        if (remindersEnabled.value) {
            com.example.notifications.StudyAlarmScheduler.scheduleDailyReminder(
                getApplication(),
                hour,
                minute
            )
        }
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        viewModelScope.launch { _userMessage.emit("Reminder time set to $formattedTime") }
    }

    fun toggleExamAlerts(enabled: Boolean) {
        repository.setExamAlertsEnabled(enabled)
        viewModelScope.launch {
            _userMessage.emit(if (enabled) "Exam alerts enabled" else "Exam alerts disabled")
        }
    }

    fun sendTestStudyNotification() {
        com.example.notifications.StudyAlarmScheduler.triggerInstantStudyReminder(getApplication())
        viewModelScope.launch { _userMessage.emit("Test study reminder notification sent!") }
    }

    fun sendTestExamNotification() {
        com.example.notifications.StudyAlarmScheduler.triggerInstantExamReminder(getApplication())
        viewModelScope.launch { _userMessage.emit("Test exam alert notification sent!") }
    }

    fun updateDailyTarget(minutes: Int) {
        if (minutes > 0) {
            repository.setDailyTargetMinutes(minutes)
        }
    }

    // User message events (Snackbars)
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    // ------------------------------------------------------------------------
    // Timer State
    // ------------------------------------------------------------------------
    private val _timerMode = MutableStateFlow(TimerMode.STUDY_25)
    val timerMode = _timerMode.asStateFlow()

    private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
    val timerStatus = _timerStatus.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(25 * 60)
    val timerRemainingSeconds = _timerRemainingSeconds.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(25 * 60)
    val timerTotalSeconds = _timerTotalSeconds.asStateFlow()

    private val _timerCategory = MutableStateFlow(StudyConstants.CAT_BE_COMP)
    val timerCategory = _timerCategory.asStateFlow()

    private val _timerSubject = MutableStateFlow("Computer Networks")
    val timerSubject = _timerSubject.asStateFlow()

    private val _timerSelectedTaskId = MutableStateFlow<Long?>(null)
    val timerSelectedTaskId = _timerSelectedTaskId.asStateFlow()

    private var timerJob: Job? = null

    fun setTimerCategory(cat: String) {
        _timerCategory.value = cat
        val subjects = StudyConstants.SUBJECTS_BY_CATEGORY[cat] ?: emptyList()
        if (subjects.isNotEmpty() && !subjects.contains(_timerSubject.value)) {
            _timerSubject.value = subjects.first()
        }
    }

    fun setTimerSubject(sub: String) {
        _timerSubject.value = sub
    }

    fun setTimerSelectedTaskId(taskId: Long?) {
        _timerSelectedTaskId.value = taskId
    }

    fun setTimerMode(mode: TimerMode, customMinutes: Int = 45) {
        _timerMode.value = mode
        val durationMins = if (mode == TimerMode.CUSTOM) customMinutes else mode.defaultMinutes
        val totalSec = durationMins * 60
        _timerTotalSeconds.value = totalSec
        _timerRemainingSeconds.value = totalSec
        pauseTimer()
        _timerStatus.value = TimerStatus.IDLE
    }

    fun startTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerStatus.value = TimerStatus.RUNNING
        timerJob = viewModelScope.launch {
            while (_timerRemainingSeconds.value > 0 && _timerStatus.value == TimerStatus.RUNNING) {
                delay(1000L)
                _timerRemainingSeconds.value -= 1
            }
            if (_timerRemainingSeconds.value <= 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        _timerStatus.value = TimerStatus.PAUSED
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        pauseTimer()
        _timerStatus.value = TimerStatus.IDLE
        _timerRemainingSeconds.value = _timerTotalSeconds.value
    }

    private fun onTimerFinished() {
        _timerStatus.value = TimerStatus.IDLE
        _timerRemainingSeconds.value = _timerTotalSeconds.value
        triggerVibration()

        val isStudyMode = _timerMode.value == TimerMode.STUDY_25 || _timerMode.value == TimerMode.CUSTOM
        val minutesStudied = _timerTotalSeconds.value / 60

        if (isStudyMode && minutesStudied > 0) {
            viewModelScope.launch {
                val session = StudySession(
                    category = _timerCategory.value,
                    subject = _timerSubject.value,
                    taskId = _timerSelectedTaskId.value,
                    durationMinutes = minutesStudied,
                    timestamp = System.currentTimeMillis(),
                    dateString = getTodayDateString(),
                    notes = "Completed ${_timerMode.value.label} Pomodoro session."
                )
                repository.insertSession(session)

                // If linked to a task, increment its actual minutes
                _timerSelectedTaskId.value?.let { tId ->
                    val task = allTasks.value.find { it.id == tId }
                    task?.let {
                        val updated = it.copy(actualMinutes = it.actualMinutes + minutesStudied)
                        repository.updateTask(updated)
                    }
                }

                _userMessage.emit("Pomodoro session completed! Logged $minutesStudied minutes of study.")
            }
        } else {
            viewModelScope.launch {
                _userMessage.emit("Break time is over! Ready to get back to studying?")
            }
        }
        com.example.notifications.NotificationHelper.showTimerAlert(getApplication(), isStudyMode, minutesStudied)
    }

    private fun triggerVibration() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ------------------------------------------------------------------------
    // Task Operations
    // ------------------------------------------------------------------------
    fun addTask(
        title: String,
        category: String,
        subject: String,
        priority: TaskPriority,
        plannedMinutes: Int,
        dateString: String = getTodayDateString(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            val task = StudyTask(
                title = title.trim(),
                category = category,
                subject = subject,
                priority = priority.name,
                plannedMinutes = plannedMinutes,
                actualMinutes = 0,
                isCompleted = false,
                dateString = dateString,
                notes = notes.trim()
            )
            repository.insertTask(task)
            _userMessage.emit("Task added successfully")
        }
    }

    fun updateTask(task: StudyTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            _userMessage.emit("Task updated")
        }
    }

    fun toggleTaskCompleted(task: StudyTask) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            val updated = task.copy(
                isCompleted = newCompleted,
                completedAt = if (newCompleted) System.currentTimeMillis() else null,
                actualMinutes = if (newCompleted && task.actualMinutes == 0) task.plannedMinutes else task.actualMinutes
            )
            repository.updateTask(updated)
            if (newCompleted) {
                // If actual minutes > 0, also log a study session for the day
                val sessionMinutes = if (task.actualMinutes > 0) task.actualMinutes else task.plannedMinutes
                repository.insertSession(
                    StudySession(
                        category = task.category,
                        subject = task.subject,
                        taskId = task.id,
                        durationMinutes = sessionMinutes,
                        timestamp = System.currentTimeMillis(),
                        dateString = task.dateString,
                        notes = "Completed task: ${task.title}"
                    )
                )
                _userMessage.emit("Task completed! Great job!")
            } else {
                _userMessage.emit("Task marked pending")
            }
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _userMessage.emit("Task deleted")
        }
    }

    // ------------------------------------------------------------------------
    // Exam Operations
    // ------------------------------------------------------------------------
    fun addExam(subject: String, category: String, examDate: String, targetScore: String) {
        viewModelScope.launch {
            val exam = ExamItem(
                subject = subject.trim(),
                category = category,
                examDate = examDate,
                targetScore = targetScore.trim(),
                isCompleted = false
            )
            repository.insertExam(exam)
            _userMessage.emit("Exam date added")
        }
    }

    fun updateExam(exam: ExamItem) {
        viewModelScope.launch {
            repository.updateExam(exam)
            _userMessage.emit("Exam updated")
        }
    }

    fun deleteExam(exam: ExamItem) {
        viewModelScope.launch {
            repository.deleteExam(exam)
            _userMessage.emit("Exam removed")
        }
    }

    // ------------------------------------------------------------------------
    // Notes & Revision Operations
    // ------------------------------------------------------------------------
    fun addRevisionTopic(
        subject: String,
        category: String,
        topicTitle: String,
        status: String,
        notes: String,
        nextRevisionDate: String
    ) {
        viewModelScope.launch {
            val topic = TopicRevision(
                subject = subject,
                category = category,
                topicTitle = topicTitle.trim(),
                status = status,
                notes = notes.trim(),
                nextRevisionDate = nextRevisionDate,
                revisionCount = if (status == "COMPLETED") 1 else 0,
                lastRevisedAt = if (status == "COMPLETED") System.currentTimeMillis() else null
            )
            repository.insertRevision(topic)
            _userMessage.emit("Topic added")
        }
    }

    fun updateRevisionTopic(topic: TopicRevision) {
        viewModelScope.launch {
            repository.updateRevision(topic)
            _userMessage.emit("Topic updated")
        }
    }

    fun markTopicRevised(topic: TopicRevision) {
        viewModelScope.launch {
            val nextDate = LocalDate.now().plusDays(3).format(dateFormatter)
            val updated = topic.copy(
                status = "COMPLETED",
                revisionCount = topic.revisionCount + 1,
                lastRevisedAt = System.currentTimeMillis(),
                nextRevisionDate = nextDate
            )
            repository.updateRevision(updated)
            _userMessage.emit("Topic revised! Next revision scheduled.")
        }
    }

    fun deleteRevisionTopic(topic: TopicRevision) {
        viewModelScope.launch {
            repository.deleteRevision(topic)
            _userMessage.emit("Topic deleted")
        }
    }

    // ------------------------------------------------------------------------
    // Calendar State
    // ------------------------------------------------------------------------
    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedCalendarDate = _selectedCalendarDate.asStateFlow()

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth = _currentYearMonth.asStateFlow()

    fun selectCalendarDate(dateStr: String) {
        _selectedCalendarDate.value = dateStr
    }

    fun changeMonth(offset: Long) {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(offset)
    }

    // ------------------------------------------------------------------------
    // Backup & Reset
    // ------------------------------------------------------------------------
    suspend fun exportJsonData(): String {
        return repository.exportToJson()
    }

    suspend fun importJsonData(json: String): Boolean {
        val success = repository.importFromJson(json)
        if (success) {
            _userMessage.emit("Data backup restored successfully!")
        } else {
            _userMessage.emit("Failed to import data. Please check JSON format.")
        }
        return success
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _userMessage.emit("All study data reset to default schedule.")
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    val repo = StudyRepository(db, application)
                    return StudyViewModel(application, repo) as T
                }
            }
    }
}
