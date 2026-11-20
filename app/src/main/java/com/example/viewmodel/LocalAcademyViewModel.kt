package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.local.LocalAcademyStore
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

/**
 * ViewModel for immediate open-access mode without requiring sign-in.
 * Does NOT access Cloud Firestore; all progress, grades, and admin edits persist locally on-device
 * until the user optionally chooses to sign in with Google for cloud sync.
 */
class LocalAcademyViewModel(
    private val store: LocalAcademyStore
) : ViewModel() {

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _uiState = MutableStateFlow(
        AcademyDashboardState(
            profile = store.loadProfile(),
            profileLoaded = true,
            isAdmin = true,
            subjects = store.loadSubjects(),
            lessons = store.loadLessons(),
            exams = store.loadExams(),
            completedLessonIds = store.loadCompletedLessonIds(),
            examResults = store.loadExamResults(),
            isCloudCatalogSeeded = false,
            isBusy = false
        )
    )
    val uiState: StateFlow<AcademyDashboardState> = _uiState.asStateFlow()

    fun saveStudentProfile(
        fullName: String,
        studentCode: String,
        department: String,
        phone: String?
    ) {
        val trimmedName = fullName.trim()
        if (trimmedName.length < 2) {
            _snackbarMessage.tryEmit("يرجى إدخال الاسم الكامل (حرفين على الأقل).")
            return
        }
        val updated = (_uiState.value.profile ?: UserProfile()).copy(
            fullName = trimmedName,
            studentCode = studentCode.trim(),
            department = department.trim().ifBlank { "الفرقة الأولى - معهد المساحة" },
            phone = phone?.trim()?.ifBlank { null }
        )
        store.saveProfile(updated)
        _uiState.update { it.copy(profile = updated) }
        _snackbarMessage.tryEmit("تم حفظ بيانات الطالب بنجاح")
    }

    fun toggleLessonCompleted(lesson: Lesson, currentlyCompleted: Boolean) {
        val nextSet = _uiState.value.completedLessonIds.toMutableSet()
        val nowCompleted = !currentlyCompleted
        if (nowCompleted) nextSet.add(lesson.id) else nextSet.remove(lesson.id)
        store.saveCompletedLessonIds(nextSet)
        _uiState.update { it.copy(completedLessonIds = nextSet) }
        _snackbarMessage.tryEmit(
            if (nowCompleted) "أحسنت! تم تسجيل إتمام الدرس وحفظ تقدمك"
            else "تم إلغاء علامة إتمام الدرس"
        )
    }

    fun submitExamAttempt(
        exam: Exam,
        score: Int,
        totalQuestions: Int,
        onSaved: (ExamResult) -> Unit
    ) {
        val safeTotal = totalQuestions.coerceAtLeast(1)
        val safeScore = score.coerceIn(0, safeTotal)
        val percentage = ((safeScore * 100.0) / safeTotal).toInt().coerceIn(0, 100)
        val passed = percentage >= exam.passingScorePercent
        val saved = ExamResult(
            id = "local_res_${System.currentTimeMillis()}",
            examId = exam.id,
            examTitle = exam.title,
            subjectId = exam.subjectId,
            subjectName = exam.subjectName,
            score = safeScore.toLong(),
            totalQuestions = safeTotal.toLong(),
            percentage = percentage.toLong(),
            passed = passed,
            status = "completed",
            submittedAt = Timestamp(Date())
        )
        val updatedList = listOf(saved) + _uiState.value.examResults
        store.saveExamResults(updatedList)
        _uiState.update { it.copy(examResults = updatedList) }
        _snackbarMessage.tryEmit("تم تصحيح الامتحان وحفظ النتيجة في ملفك الشخصي")
        onSaved(saved)
    }

    fun resetCurriculum() {
        store.resetCurriculumToDefaults()
        _uiState.update {
            it.copy(
                subjects = store.loadSubjects(),
                lessons = store.loadLessons(),
                exams = store.loadExams()
            )
        }
        _snackbarMessage.tryEmit("تمت تهيئة المنهج الكامل (8 مواد و16 درساً و8 امتحانات)")
    }

    fun saveSubject(subject: Subject, isEdit: Boolean) {
        val id = subject.id.ifBlank { "subj_${System.currentTimeMillis()}" }
        val normalized = subject.copy(id = id, published = true)
        val current = _uiState.value.subjects.toMutableList()
        if (isEdit) {
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) current[idx] = normalized else current.add(normalized)
        } else {
            current.add(normalized)
        }
        val sorted = current.sortedBy { it.orderIndex }
        store.saveSubjects(sorted)
        _uiState.update { it.copy(subjects = sorted) }
        _snackbarMessage.tryEmit(if (isEdit) "تم تعديل المادة بنجاح" else "تمت إضافة المادة بنجاح")
    }

    fun deleteSubject(subjectId: String) {
        val updated = _uiState.value.subjects.filterNot { it.id == subjectId }
        store.saveSubjects(updated)
        _uiState.update { it.copy(subjects = updated) }
        _snackbarMessage.tryEmit("تم حذف المادة الدراسية")
    }

    fun saveLesson(lesson: Lesson, isEdit: Boolean) {
        val id = lesson.id.ifBlank { "les_${System.currentTimeMillis()}" }
        val normalized = lesson.copy(id = id, published = true)
        val current = _uiState.value.lessons.toMutableList()
        if (isEdit) {
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) current[idx] = normalized else current.add(normalized)
        } else {
            current.add(normalized)
        }
        val sorted = current.sortedBy { it.orderIndex }
        store.saveLessons(sorted)
        _uiState.update { it.copy(lessons = sorted) }
        _snackbarMessage.tryEmit(if (isEdit) "تم تحديث الدرس بنجاح" else "تمت إضافة الدرس بنجاح")
    }

    fun deleteLesson(lessonId: String) {
        val updated = _uiState.value.lessons.filterNot { it.id == lessonId }
        store.saveLessons(updated)
        _uiState.update { it.copy(lessons = updated) }
        _snackbarMessage.tryEmit("تم حذف الدرس")
    }

    fun saveExam(exam: Exam, isEdit: Boolean) {
        val id = exam.id.ifBlank { "exam_${System.currentTimeMillis()}" }
        val normalized = exam.copy(id = id, published = true)
        val current = _uiState.value.exams.toMutableList()
        if (isEdit) {
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) current[idx] = normalized else current.add(normalized)
        } else {
            current.add(normalized)
        }
        store.saveExams(current)
        _uiState.update { it.copy(exams = current) }
        _snackbarMessage.tryEmit(if (isEdit) "تم تحديث الامتحان بنجاح" else "تمت إضافة الامتحان بنجاح")
    }

    fun deleteExam(examId: String) {
        val updated = _uiState.value.exams.filterNot { it.id == examId }
        store.saveExams(updated)
        _uiState.update { it.copy(exams = updated) }
        _snackbarMessage.tryEmit("تم حذف الامتحان")
    }

    fun notifyUser(message: String) {
        _snackbarMessage.tryEmit(message)
    }
}
