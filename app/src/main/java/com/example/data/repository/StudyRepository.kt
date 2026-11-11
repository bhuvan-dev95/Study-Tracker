package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.model.ExamItem
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.TopicRevision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class StudyRepository(
    private val database: AppDatabase,
    context: Context
) {
    private val taskDao = database.taskDao()
    private val sessionDao = database.studySessionDao()
    private val examDao = database.examDao()
    private val topicDao = database.topicRevisionDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_tracker_prefs", Context.MODE_PRIVATE)

    private val _dailyTargetMinutes =
        MutableStateFlow(prefs.getInt("daily_target_minutes", 120)) // Default 2 hours = 120m
    val dailyTargetMinutes = _dailyTargetMinutes.asStateFlow()

    private val _isDarkMode =
        MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDailyTargetMinutes(minutes: Int) {
        prefs.edit().putInt("daily_target_minutes", minutes).apply()
        _dailyTargetMinutes.value = minutes
    }

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", dark).apply()
        _isDarkMode.value = dark
    }

    private val _remindersEnabled =
        MutableStateFlow(prefs.getBoolean("reminders_enabled", true))
    val remindersEnabled = _remindersEnabled.asStateFlow()

    private val _reminderHour =
        MutableStateFlow(prefs.getInt("reminder_hour", 9))
    val reminderHour = _reminderHour.asStateFlow()

    private val _reminderMinute =
        MutableStateFlow(prefs.getInt("reminder_minute", 0))
    val reminderMinute = _reminderMinute.asStateFlow()

    private val _examAlertsEnabled =
        MutableStateFlow(prefs.getBoolean("exam_alerts_enabled", true))
    val examAlertsEnabled = _examAlertsEnabled.asStateFlow()

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
        _remindersEnabled.value = enabled
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
        _reminderHour.value = hour
        _reminderMinute.value = minute
    }

    fun setExamAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("exam_alerts_enabled", enabled).apply()
        _examAlertsEnabled.value = enabled
    }

    // Tasks
    val allTasks: Flow<List<StudyTask>> = taskDao.getAllTasks()
    suspend fun insertTask(task: StudyTask) = withContext(Dispatchers.IO) { taskDao.insertTask(task) }
    suspend fun updateTask(task: StudyTask) = withContext(Dispatchers.IO) { taskDao.updateTask(task) }
    suspend fun deleteTask(task: StudyTask) = withContext(Dispatchers.IO) { taskDao.deleteTask(task) }

    // Sessions
    val allSessions: Flow<List<StudySession>> = sessionDao.getAllSessions()
    suspend fun insertSession(session: StudySession) = withContext(Dispatchers.IO) { sessionDao.insertSession(session) }
    suspend fun deleteSession(session: StudySession) = withContext(Dispatchers.IO) { sessionDao.deleteSession(session) }

    // Exams
    val allExams: Flow<List<ExamItem>> = examDao.getAllExams()
    suspend fun insertExam(exam: ExamItem) = withContext(Dispatchers.IO) { examDao.insertExam(exam) }
    suspend fun updateExam(exam: ExamItem) = withContext(Dispatchers.IO) { examDao.updateExam(exam) }
    suspend fun deleteExam(exam: ExamItem) = withContext(Dispatchers.IO) { examDao.deleteExam(exam) }

    // Topics & Revisions
    val allRevisions: Flow<List<TopicRevision>> = topicDao.getAllRevisions()
    suspend fun insertRevision(topic: TopicRevision) = withContext(Dispatchers.IO) { topicDao.insertRevision(topic) }
    suspend fun updateRevision(topic: TopicRevision) = withContext(Dispatchers.IO) { topicDao.updateRevision(topic) }
    suspend fun deleteRevision(topic: TopicRevision) = withContext(Dispatchers.IO) { topicDao.deleteRevision(topic) }

    // Reset Data
    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        taskDao.deleteAll()
        sessionDao.deleteAll()
        examDao.deleteAll()
        topicDao.deleteAll()
        AppDatabase.seedInitialData(database)
        setDailyTargetMinutes(120)
    }

    // Export Data to JSON String
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("dailyTargetMinutes", _dailyTargetMinutes.value)

        val tasksArray = JSONArray()
        taskDao.getAllTasksSnapshot().forEach { task ->
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("category", task.category)
            obj.put("subject", task.subject)
            obj.put("priority", task.priority)
            obj.put("plannedMinutes", task.plannedMinutes)
            obj.put("actualMinutes", task.actualMinutes)
            obj.put("isCompleted", task.isCompleted)
            obj.put("dateString", task.dateString)
            obj.put("notes", task.notes)
            obj.put("completedAt", task.completedAt ?: 0L)
            obj.put("createdAt", task.createdAt)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        val sessionsArray = JSONArray()
        sessionDao.getAllSessionsSnapshot().forEach { session ->
            val obj = JSONObject()
            obj.put("id", session.id)
            obj.put("category", session.category)
            obj.put("subject", session.subject)
            obj.put("taskId", session.taskId ?: -1L)
            obj.put("durationMinutes", session.durationMinutes)
            obj.put("timestamp", session.timestamp)
            obj.put("dateString", session.dateString)
            obj.put("notes", session.notes)
            sessionsArray.put(obj)
        }
        root.put("sessions", sessionsArray)

        val examsArray = JSONArray()
        examDao.getAllExamsSnapshot().forEach { exam ->
            val obj = JSONObject()
            obj.put("id", exam.id)
            obj.put("subject", exam.subject)
            obj.put("category", exam.category)
            obj.put("examDate", exam.examDate)
            obj.put("targetScore", exam.targetScore)
            obj.put("isCompleted", exam.isCompleted)
            examsArray.put(obj)
        }
        root.put("exams", examsArray)

        val topicsArray = JSONArray()
        topicDao.getAllRevisionsSnapshot().forEach { topic ->
            val obj = JSONObject()
            obj.put("id", topic.id)
            obj.put("subject", topic.subject)
            obj.put("category", topic.category)
            obj.put("topicTitle", topic.topicTitle)
            obj.put("status", topic.status)
            obj.put("notes", topic.notes)
            obj.put("nextRevisionDate", topic.nextRevisionDate)
            obj.put("revisionCount", topic.revisionCount)
            obj.put("lastRevisedAt", topic.lastRevisedAt ?: 0L)
            topicsArray.put(obj)
        }
        root.put("topics", topicsArray)

        root.toString(2)
    }

    // Import Data from JSON String
    suspend fun importFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("dailyTargetMinutes")) {
                setDailyTargetMinutes(root.getInt("dailyTargetMinutes"))
            }

            if (root.has("tasks")) {
                val tasksArray = root.getJSONArray("tasks")
                val tasksList = mutableListOf<StudyTask>()
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)
                    tasksList.add(
                        StudyTask(
                            id = if (obj.has("id")) obj.getLong("id") else 0,
                            title = obj.getString("title"),
                            category = obj.getString("category"),
                            subject = obj.getString("subject"),
                            priority = obj.optString("priority", "MEDIUM"),
                            plannedMinutes = obj.optInt("plannedMinutes", 60),
                            actualMinutes = obj.optInt("actualMinutes", 0),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            dateString = obj.optString("dateString", ""),
                            notes = obj.optString("notes", ""),
                            completedAt = if (obj.optLong("completedAt", 0L) > 0) obj.getLong("completedAt") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                taskDao.deleteAll()
                taskDao.insertAll(tasksList)
            }

            if (root.has("sessions")) {
                val sessionsArray = root.getJSONArray("sessions")
                val sessionsList = mutableListOf<StudySession>()
                for (i in 0 until sessionsArray.length()) {
                    val obj = sessionsArray.getJSONObject(i)
                    val tId = obj.optLong("taskId", -1L)
                    sessionsList.add(
                        StudySession(
                            id = if (obj.has("id")) obj.getLong("id") else 0,
                            category = obj.getString("category"),
                            subject = obj.getString("subject"),
                            taskId = if (tId > 0) tId else null,
                            durationMinutes = obj.getInt("durationMinutes"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            dateString = obj.optString("dateString", ""),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                sessionDao.deleteAll()
                sessionDao.insertAll(sessionsList)
            }

            if (root.has("exams")) {
                val examsArray = root.getJSONArray("exams")
                val examsList = mutableListOf<ExamItem>()
                for (i in 0 until examsArray.length()) {
                    val obj = examsArray.getJSONObject(i)
                    examsList.add(
                        ExamItem(
                            id = if (obj.has("id")) obj.getLong("id") else 0,
                            subject = obj.getString("subject"),
                            category = obj.optString("category", "BE Computer Engineering"),
                            examDate = obj.getString("examDate"),
                            targetScore = obj.optString("targetScore", ""),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
                examDao.deleteAll()
                examDao.insertAll(examsList)
            }

            if (root.has("topics")) {
                val topicsArray = root.getJSONArray("topics")
                val topicsList = mutableListOf<TopicRevision>()
                for (i in 0 until topicsArray.length()) {
                    val obj = topicsArray.getJSONObject(i)
                    topicsList.add(
                        TopicRevision(
                            id = if (obj.has("id")) obj.getLong("id") else 0,
                            subject = obj.getString("subject"),
                            category = obj.getString("category"),
                            topicTitle = obj.getString("topicTitle"),
                            status = obj.optString("status", "NOT_STARTED"),
                            notes = obj.optString("notes", ""),
                            nextRevisionDate = obj.optString("nextRevisionDate", ""),
                            revisionCount = obj.optInt("revisionCount", 0),
                            lastRevisedAt = if (obj.optLong("lastRevisedAt", 0L) > 0) obj.getLong("lastRevisedAt") else null
                        )
                    )
                }
                topicDao.deleteAll()
                topicDao.insertAll(topicsList)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
