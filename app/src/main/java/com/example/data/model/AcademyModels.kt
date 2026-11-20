package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val studentCode: String = "",
    val department: String = "الفرقة الأولى - معهد المساحة",
    val phone: String? = null,
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val instructor: String = "",
    val iconKey: String = "terrain",
    val orderIndex: Long = 0L,
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Lesson(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val title: String = "",
    val summary: String = "",
    val contentText: String = "",
    val imageKey: String? = null,
    val videoTitle: String? = null,
    val videoUrl: String? = null,
    val pdfTitle: String? = null,
    val pdfUrl: String? = null,
    val durationMinutes: Long = 20L,
    val orderIndex: Long = 0L,
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamQuestion(
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Long = 0L,
    val explanation: String = ""
)

@Keep
@IgnoreExtraProperties
data class Exam(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val title: String = "",
    val description: String? = null,
    val durationMinutes: Long = 15L,
    val passingScorePercent: Long = 60L,
    val questions: List<ExamQuestion> = emptyList(),
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LessonProgress(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val lessonId: String = "",
    val lessonTitle: String = "",
    val completed: Boolean = true,
    @ServerTimestamp val completedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamResult(
    @DocumentId val id: String = "",
    val examId: String = "",
    val examTitle: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val score: Long = 0L,
    val totalQuestions: Long = 1L,
    val percentage: Long = 0L,
    val passed: Boolean = false,
    val status: String = "completed",
    @ServerTimestamp val submittedAt: Timestamp? = null
)
