package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExamItem
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.TopicRevision
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM tasks WHERE dateString = :date ORDER BY isCompleted ASC, createdAt DESC")
    fun getTasksForDate(date: String): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<StudyTask>)

    @Update
    suspend fun updateTask(task: StudyTask)

    @Delete
    suspend fun deleteTask(task: StudyTask)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSnapshot(): List<StudyTask>
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE dateString = :date ORDER BY timestamp DESC")
    fun getSessionsForDate(date: String): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<StudySession>)

    @Delete
    suspend fun deleteSession(session: StudySession)

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM study_sessions")
    suspend fun getAllSessionsSnapshot(): List<StudySession>
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<ExamItem>)

    @Update
    suspend fun updateExam(exam: ExamItem)

    @Delete
    suspend fun deleteExam(exam: ExamItem)

    @Query("DELETE FROM exams")
    suspend fun deleteAll()

    @Query("SELECT * FROM exams")
    suspend fun getAllExamsSnapshot(): List<ExamItem>
}

@Dao
interface TopicRevisionDao {
    @Query("SELECT * FROM topic_revisions ORDER BY topicTitle ASC")
    fun getAllRevisions(): Flow<List<TopicRevision>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: TopicRevision): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(revisions: List<TopicRevision>)

    @Update
    suspend fun updateRevision(revision: TopicRevision)

    @Delete
    suspend fun deleteRevision(revision: TopicRevision)

    @Query("DELETE FROM topic_revisions")
    suspend fun deleteAll()

    @Query("SELECT * FROM topic_revisions")
    suspend fun getAllRevisionsSnapshot(): List<TopicRevision>
}
