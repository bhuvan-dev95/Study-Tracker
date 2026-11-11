package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object StudyConstants {
    const val CAT_BE_COMP = "BE Computer Engineering"
    const val CAT_GPSC = "GPSC Class 1 & 2"
    const val CAT_SSC_CGL = "SSC CGL"

    val CATEGORIES = listOf(CAT_BE_COMP, CAT_GPSC, CAT_SSC_CGL)

    val SUBJECTS_BY_CATEGORY = mapOf(
        CAT_BE_COMP to listOf(
            "Computer Networks",
            "System Software",
            "Object-Oriented Programming (Backlog)",
            "Python for Data Science",
            "Project Management",
            "Microprocessor and Interfacing",
            "Web Application Development"
        ),
        CAT_GPSC to listOf(
            "Indian Polity",
            "History",
            "Geography",
            "Indian Economy",
            "General Science",
            "Gujarat General Knowledge",
            "Current Affairs"
        ),
        CAT_SSC_CGL to listOf(
            "Quantitative Aptitude",
            "Reasoning",
            "English",
            "General Awareness"
        )
    )

    val INITIAL_EXAMS = listOf(
        Triple("Computer Networks", "2026-12-02", CAT_BE_COMP),
        Triple("System Software", "2026-12-04", CAT_BE_COMP),
        Triple("OOP Backlog", "2026-12-05", CAT_BE_COMP),
        Triple("Python for Data Science", "2026-12-09", CAT_BE_COMP),
        Triple("Project Management", "2026-12-15", CAT_BE_COMP),
        Triple("Microprocessor and Interfacing", "2026-12-17", CAT_BE_COMP),
        Triple("Web Application Development", "2026-12-22", CAT_BE_COMP)
    )
}

enum class TaskPriority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

enum class RevisionStatus(val label: String) {
    NOT_STARTED("Not Started"),
    LEARNING("Learning"),
    COMPLETED("Completed")
}

@Entity(tableName = "tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val subject: String,
    val priority: String = TaskPriority.MEDIUM.name,
    val plannedMinutes: Int = 60,
    val actualMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val dateString: String, // YYYY-MM-DD
    val notes: String = "",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val subject: String,
    val taskId: Long? = null,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // YYYY-MM-DD
    val notes: String = ""
)

@Entity(tableName = "exams")
data class ExamItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val category: String = StudyConstants.CAT_BE_COMP,
    val examDate: String, // YYYY-MM-DD
    val targetScore: String = "Pass with Distinction",
    val isCompleted: Boolean = false
)

@Entity(tableName = "topic_revisions")
data class TopicRevision(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val category: String,
    val topicTitle: String,
    val status: String = RevisionStatus.NOT_STARTED.name,
    val notes: String = "",
    val nextRevisionDate: String = "", // YYYY-MM-DD
    val revisionCount: Int = 0,
    val lastRevisedAt: Long? = null
)
