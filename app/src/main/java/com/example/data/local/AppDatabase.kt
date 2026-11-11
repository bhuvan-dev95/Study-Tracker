package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ExamItem
import com.example.data.model.StudyConstants
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.TaskPriority
import com.example.data.model.TopicRevision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [StudyTask::class, StudySession::class, ExamItem::class, TopicRevision::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun examDao(): ExamDao
    abstract fun topicRevisionDao(): TopicRevisionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial pre-seeded data
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedInitialData(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // 1. Seed University Exams
            val exams = StudyConstants.INITIAL_EXAMS.map { (sub, date, cat) ->
                ExamItem(
                    subject = sub,
                    category = cat,
                    examDate = date,
                    targetScore = "Target: 80+ / 100",
                    isCompleted = false
                )
            }
            database.examDao().insertAll(exams)

            // 2. Seed Initial Study Tasks for student
            val initialTasks = listOf(
                StudyTask(
                    title = "Solve Subnetting and CIDR Numerical Problems",
                    category = StudyConstants.CAT_BE_COMP,
                    subject = "Computer Networks",
                    priority = TaskPriority.HIGH.name,
                    plannedMinutes = 60,
                    actualMinutes = 45,
                    isCompleted = true,
                    dateString = todayStr,
                    notes = "Covered IPv4 address classes, CIDR notation, and subnet masks."
                ),
                StudyTask(
                    title = "Revise Lexical Analyzer & Parser Phase (Compilers)",
                    category = StudyConstants.CAT_BE_COMP,
                    subject = "System Software",
                    priority = TaskPriority.HIGH.name,
                    plannedMinutes = 60,
                    actualMinutes = 0,
                    isCompleted = false,
                    dateString = todayStr,
                    notes = "Focus on LL(1) parsing tables and Ambiguity resolution."
                ),
                StudyTask(
                    title = "Fundamental Rights Articles 12-35 Revision",
                    category = StudyConstants.CAT_GPSC,
                    subject = "Indian Polity",
                    priority = TaskPriority.MEDIUM.name,
                    plannedMinutes = 45,
                    actualMinutes = 30,
                    isCompleted = true,
                    dateString = todayStr,
                    notes = "M. Laxmikanth Chapter 7 notes and previous year questions."
                ),
                StudyTask(
                    title = "Practice Time & Work and Pipe Cisterns (50 questions)",
                    category = StudyConstants.CAT_SSC_CGL,
                    subject = "Quantitative Aptitude",
                    priority = TaskPriority.MEDIUM.name,
                    plannedMinutes = 60,
                    actualMinutes = 0,
                    isCompleted = false,
                    dateString = todayStr,
                    notes = "Focus on efficiency-based problem shortcuts."
                ),
                StudyTask(
                    title = "C++ OOP Concepts (Polymorphism & Virtual Functions)",
                    category = StudyConstants.CAT_BE_COMP,
                    subject = "Object-Oriented Programming (Backlog)",
                    priority = TaskPriority.HIGH.name,
                    plannedMinutes = 45,
                    actualMinutes = 0,
                    isCompleted = false,
                    dateString = todayStr,
                    notes = "Key university exam backlog topic. Must master pure virtual functions."
                )
            )
            database.taskDao().insertAll(initialTasks)

            // 3. Seed Study Sessions for today to display initial working stats
            val initialSessions = listOf(
                StudySession(
                    category = StudyConstants.CAT_BE_COMP,
                    subject = "Computer Networks",
                    durationMinutes = 45,
                    timestamp = System.currentTimeMillis() - 7200000,
                    dateString = todayStr,
                    notes = "Pomodoro Session 1: Subnetting calculations."
                ),
                StudySession(
                    category = StudyConstants.CAT_GPSC,
                    subject = "Indian Polity",
                    durationMinutes = 30,
                    timestamp = System.currentTimeMillis() - 3600000,
                    dateString = todayStr,
                    notes = "Pomodoro Session 2: Articles 14 to 21."
                )
            )
            database.studySessionDao().insertAll(initialSessions)

            // 4. Seed Notes & Revision Topics
            val initialTopics = listOf(
                TopicRevision(
                    subject = "Computer Networks",
                    category = StudyConstants.CAT_BE_COMP,
                    topicTitle = "OSI Model vs TCP/IP Layers",
                    status = "COMPLETED",
                    notes = "Physical, Data Link, Network, Transport, Session, Presentation, Application. Encapsulation & PDU differences.",
                    nextRevisionDate = todayStr,
                    revisionCount = 2,
                    lastRevisedAt = System.currentTimeMillis() - 86400000
                ),
                TopicRevision(
                    subject = "System Software",
                    category = StudyConstants.CAT_BE_COMP,
                    topicTitle = "Macro Processors & Two-Pass Assemblers",
                    status = "LEARNING",
                    notes = "Pass 1: Build Symbol Table (SYMTAB), Literals (LITTAB). Pass 2: Generate Object Code.",
                    nextRevisionDate = todayStr,
                    revisionCount = 1,
                    lastRevisedAt = System.currentTimeMillis() - 43200000
                ),
                TopicRevision(
                    subject = "Indian Polity",
                    category = StudyConstants.CAT_GPSC,
                    topicTitle = "Preamble & Salient Features of Constitution",
                    status = "COMPLETED",
                    notes = "Sovereign Socialist Secular Democratic Republic. 42nd Amendment 1976 additions.",
                    nextRevisionDate = todayStr,
                    revisionCount = 3,
                    lastRevisedAt = System.currentTimeMillis() - 172800000
                ),
                TopicRevision(
                    subject = "Quantitative Aptitude",
                    category = StudyConstants.CAT_SSC_CGL,
                    topicTitle = "Percentage to Fraction Conversions & Profit Loss",
                    status = "NOT_STARTED",
                    notes = "Master 1/7 = 14.28%, 1/8 = 12.5%, 1/9 = 11.11%, 1/11 = 9.09% for fast math.",
                    nextRevisionDate = todayStr,
                    revisionCount = 0
                )
            )
            database.topicRevisionDao().insertAll(initialTopics)
        }
    }
}
