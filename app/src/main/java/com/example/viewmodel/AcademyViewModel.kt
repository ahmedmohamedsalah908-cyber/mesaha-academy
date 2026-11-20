package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AcademyRepository
import com.example.data.model.DefaultCurriculum
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Lesson
import com.example.data.model.LessonProgress
import com.example.data.model.Subject
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AcademyDashboardState(
    val profile: UserProfile? = null,
    val profileLoaded: Boolean = false,
    val isAdmin: Boolean = false,
    val subjects: List<Subject> = DefaultCurriculum.subjects,
    val lessons: List<Lesson> = DefaultCurriculum.lessons,
    val exams: List<Exam> = DefaultCurriculum.exams,
    val completedLessonIds: Set<String> = emptySet(),
    val examResults: List<ExamResult> = emptyList(),
    val isCloudCatalogSeeded: Boolean = false,
    val isBusy: Boolean = false,
    val errorMessage: String? = null
) {
    val totalLessonsCount: Int get() = lessons.size
    val completedLessonsCount: Int get() = lessons.count { it.id in completedLessonIds }
    val overallProgressFraction: Float
        get() = if (lessons.isEmpty()) 0f else completedLessonsCount.toFloat() / lessons.size.toFloat()
    val overallProgressPercent: Int get() = (overallProgressFraction * 100).toInt()

    val averageExamScore: Int
        get() = if (examResults.isEmpty()) 0 else examResults.map { it.percentage }.average().toInt()

    val passedExamsCount: Int
        get() = examResults.filter { it.passed }.map { it.examId }.distinct().size

    fun lessonsForSubject(subjectId: String): List<Lesson> =
        lessons.filter { it.subjectId == subjectId }.sortedBy { it.orderIndex }

    fun examsForSubject(subjectId: String): List<Exam> =
        exams.filter { it.subjectId == subjectId }

    fun subjectProgressFraction(subjectId: String): Float {
        val subjectLessons = lessonsForSubject(subjectId)
        if (subjectLessons.isEmpty()) return 0f
        val done = subjectLessons.count { it.id in completedLessonIds }
        return done.toFloat() / subjectLessons.size.toFloat()
    }

    fun bestExamResult(examId: String): ExamResult? =
        examResults.filter { it.examId == examId }.maxByOrNull { it.percentage }
}

class AcademyViewModel(
    private val repository: AcademyRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    private val isEmailVerified: Boolean,
    val googleDisplayName: String,
    val googlePhotoUrl: String?
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    private val _isBusy = MutableStateFlow(false)
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error -> handleStreamError(error, "profile") { emit(UiState.Error(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val subjectsState: StateFlow<UiState<List<Subject>>> =
        repository.observeSubjects()
            .map<List<Subject>, UiState<List<Subject>>> { UiState.Success(it) }
            .catch { error -> handleStreamError(error, "subjects") { emit(UiState.Error(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val lessonsState: StateFlow<UiState<List<Lesson>>> =
        repository.observeLessons()
            .map<List<Lesson>, UiState<List<Lesson>>> { UiState.Success(it) }
            .catch { error -> handleStreamError(error, "lessons") { emit(UiState.Error(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val examsState: StateFlow<UiState<List<Exam>>> =
        repository.observeExams()
            .map<List<Exam>, UiState<List<Exam>>> { UiState.Success(it) }
            .catch { error -> handleStreamError(error, "exams") { emit(UiState.Error(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val progressState: StateFlow<UiState<List<LessonProgress>>> =
        repository.observeLessonProgress(currentUserId)
            .map<List<LessonProgress>, UiState<List<LessonProgress>>> { UiState.Success(it) }
            .catch { error -> handleStreamError(error, "lessonProgress") { emit(UiState.Error(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val examResultsState: StateFlow<UiState<List<ExamResult>>> =
        repository.observeExamResults(currentUserId)
            .map<List<ExamResult>, UiState<List<ExamResult>>> { UiState.Success(it) }
            .catch { error -> handleStreamError(error, "examResults") { emit(UiState.Error(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val catalogFlow = combine(subjectsState, lessonsState, examsState) { s, l, e ->
        Triple(s, l, e)
    }

    private val studentDataFlow = combine(profileState, progressState, examResultsState) { p, pr, er ->
        Triple(p, pr, er)
    }

    val uiState: StateFlow<AcademyDashboardState> = combine(
        catalogFlow,
        studentDataFlow,
        _isAdmin,
        _isBusy
    ) { catalog, student, admin, busy ->
        val (subjRes, lesRes, examRes) = catalog
        val (profRes, progRes, resRes) = student

        val cloudSubjects = (subjRes as? UiState.Success)?.data.orEmpty()
        val cloudLessons = (lesRes as? UiState.Success)?.data.orEmpty()
        val cloudExams = (examRes as? UiState.Success)?.data.orEmpty()

        val cloudSeeded = cloudSubjects.isNotEmpty()
        val effectiveSubjects = if (cloudSeeded) cloudSubjects else DefaultCurriculum.subjects
        val effectiveLessons = if (cloudSeeded) cloudLessons else DefaultCurriculum.lessons
        val effectiveExams = if (cloudSeeded) cloudExams else DefaultCurriculum.exams

        val completedIds = (progRes as? UiState.Success)?.data
            ?.filter { it.completed }
            ?.map { it.lessonId }
            ?.toSet()
            ?: emptySet()

        val results = (resRes as? UiState.Success)?.data.orEmpty()
        val profile = (profRes as? UiState.Success)?.data
        val profileLoaded = profRes !is UiState.Loading

        val firstError = listOf(subjRes, lesRes, examRes, profRes, progRes, resRes)
            .filterIsInstance<UiState.Error>()
            .firstOrNull()?.message

        AcademyDashboardState(
            profile = profile,
            profileLoaded = profileLoaded,
            isAdmin = admin,
            subjects = effectiveSubjects,
            lessons = effectiveLessons,
            exams = effectiveExams,
            completedLessonIds = completedIds,
            examResults = results,
            isCloudCatalogSeeded = cloudSeeded,
            isBusy = busy,
            errorMessage = firstError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AcademyDashboardState()
    )

    init {
        viewModelScope.launch {
            val admin = repository.isUserAdmin(currentUserId, currentUserEmail, isEmailVerified)
            _isAdmin.value = admin
            if (admin) {
                // Auto-seed the 8-subject curriculum into Firestore on first admin launch if empty
                repository.seedDefaultCurriculum(forceOverwrite = false)
            }
        }
    }

    private suspend fun handleStreamError(
        error: Throwable,
        streamName: String,
        onEmitError: suspend (String) -> Unit
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        onEmitError(rootError.localizedMessage ?: "تعذر تحميل البيانات من الخادم.")
    }

    fun saveStudentProfile(
        fullName: String,
        studentCode: String,
        department: String,
        phone: String?,
        isExisting: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createOrUpdateUserProfile(
                fullName = fullName,
                email = currentUserEmail,
                studentCode = studentCode,
                department = department,
                phone = phone,
                photoUrl = googlePhotoUrl,
                isExistingProfile = isExisting
            )
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("تم حفظ بيانات الطالب بنجاح")
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حفظ البيانات")
            }
        }
    }

    fun toggleLessonCompleted(lesson: Lesson, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            val targetState = !currentlyCompleted
            val result = repository.setLessonCompleted(lesson, targetState)
            result.onSuccess {
                _snackbarMessage.tryEmit(
                    if (targetState) "أحسنت! تم تسجيل إتمام الدرس وحفظ تقدمك"
                    else "تم إلغاء علامة إتمام الدرس"
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر تحديث حالة الدرس")
            }
        }
    }

    fun submitExamAttempt(
        exam: Exam,
        score: Int,
        totalQuestions: Int,
        onSaved: (ExamResult) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveExamResult(exam, score, totalQuestions)
            _isBusy.value = false
            result.onSuccess { saved ->
                _snackbarMessage.tryEmit("تم تصحيح الامتحان وحفظ النتيجة في ملفك الشخصي")
                onSaved(saved)
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حفظ نتيجة الامتحان")
            }
        }
    }

    // --- ADMIN ACTIONS ---
    fun seedCurriculum(forceOverwrite: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.seedDefaultCurriculum(forceOverwrite = forceOverwrite)
            _isBusy.value = false
            result.onSuccess { count ->
                _snackbarMessage.tryEmit(
                    if (count > 0) "تمت تهيئة المنهج الكامل (8 مواد و16 درساً و8 امتحانات) في قاعدة البيانات"
                    else "المنهج مهيأ بالفعل في قاعدة البيانات"
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر تهيئة المنهج")
            }
        }
    }

    fun saveSubject(subject: Subject, isEdit: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.upsertSubject(subject, isEdit)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit(if (isEdit) "تم تعديل المادة بنجاح" else "تمت إضافة المادة بنجاح")
                onDone()
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حفظ المادة")
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteSubject(subjectId)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("تم حذف المادة الدراسية")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حذف المادة")
            }
        }
    }

    fun saveLesson(lesson: Lesson, isEdit: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.upsertLesson(lesson, isEdit)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit(if (isEdit) "تم تحديث الدرس بنجاح" else "تمت إضافة الدرس بنجاح")
                onDone()
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حفظ الدرس")
            }
        }
    }

    fun deleteLesson(lessonId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteLesson(lessonId)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("تم حذف الدرس")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حذف الدرس")
            }
        }
    }

    fun saveExam(exam: Exam, isEdit: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.upsertExam(exam, isEdit)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit(if (isEdit) "تم تحديث الامتحان بنجاح" else "تمت إضافة الامتحان بنجاح")
                onDone()
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حفظ الامتحان")
            }
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteExam(examId)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("تم حذف الامتحان")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "تعذر حذف الامتحان")
            }
        }
    }

    fun notifyUser(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    private companion object {
        const val TAG = "AcademyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
