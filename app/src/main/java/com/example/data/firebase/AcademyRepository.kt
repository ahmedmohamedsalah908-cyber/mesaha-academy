package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.DefaultCurriculum
import com.example.data.model.Exam
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamResult
import com.example.data.model.Lesson
import com.example.data.model.LessonProgress
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val BOOTSTRAPPED_ADMIN_EMAIL = "ahmedmohamedsalah908@gmail.com"
private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

class AcademyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Always initialize Firestore using the provisioned named database ID
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً.")
        }
        return uid
    }

    private fun safeDocId(prefix: String, existingId: String? = null): String {
        if (!existingId.isNullOrBlank() && VALID_ID_REGEX.matches(existingId)) {
            return existingId
        }
        return "${prefix}_${System.currentTimeMillis()}"
    }

    // --- ADMIN ROLE CHECK ---
    suspend fun isUserAdmin(uid: String, email: String?, emailVerified: Boolean): Boolean {
        if (email != null && email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && emailVerified) {
            return true
        }
        return try {
            val snapshot = db.collection("admins").document(uid).get().await()
            snapshot.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "admins/$uid")
            false
        }
    }

    // --- USER PROFILE ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createOrUpdateUserProfile(
        fullName: String,
        email: String,
        studentCode: String,
        department: String,
        phone: String?,
        photoUrl: String?,
        isExistingProfile: Boolean
    ): Result<Unit> {
        val trimmedName = fullName.trim()
        val trimmedCode = studentCode.trim()
        val trimmedDept = department.trim().ifBlank { "الفرقة الأولى - معهد المساحة" }
        val cleanPhone = phone?.trim()?.takeIf { it.isNotEmpty() }
        val cleanPhoto = photoUrl?.trim()?.takeIf { it.isNotEmpty() }

        if (trimmedName.length !in 2..80) {
            return Result.failure(IllegalArgumentException("يرجى إدخال الاسم الكامل (من حرفين إلى 80 حرفاً)."))
        }
        if (trimmedCode.length > 30) {
            return Result.failure(IllegalArgumentException("رقم الجلوس أو الكود الجامعي طويل جداً."))
        }
        if (cleanPhone != null && cleanPhone.length > 25) {
            return Result.failure(IllegalArgumentException("رقم الهاتف يجب ألا يتجاوز 25 رقماً."))
        }

        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val snapshot = if (isExistingProfile) null else docRef.get().await()
            val exists = isExistingProfile || (snapshot?.exists() == true)

            if (exists) {
                val updateMap = mutableMapOf<String, Any>(
                    "fullName" to trimmedName,
                    "email" to email.trim().take(120),
                    "studentCode" to trimmedCode,
                    "department" to trimmedDept.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanPhone != null) updateMap["phone"] = cleanPhone
                if (cleanPhoto != null) updateMap["photoUrl"] = cleanPhoto.take(500)
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "fullName" to trimmedName,
                    "email" to email.trim().take(120),
                    "studentCode" to trimmedCode,
                    "department" to trimmedDept.take(100),
                    "phone" to cleanPhone,
                    "photoUrl" to cleanPhoto?.take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- CATALOG OBSERVATION (strictly filtered by published == true for rule alignment) ---
    fun observeSubjects(): Flow<List<Subject>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("subjects")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Subject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.orderIndex }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, "subjects")
                    }
                    throw error
                }
        )
    }

    fun observeLessons(): Flow<List<Lesson>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("lessons")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Lesson::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.orderIndex }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, "lessons")
                    }
                    throw error
                }
        )
    }

    fun observeExams(): Flow<List<Exam>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("exams")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Exam::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, "exams")
                    }
                    throw error
                }
        )
    }

    // --- STUDENT PROGRESS & EXAM RESULTS ---
    fun observeLessonProgress(userId: String): Flow<List<LessonProgress>> = flow {
        val path = "users/$userId/lessonProgress"
        emitAll(
            db.collection("users").document(userId).collection("lessonProgress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LessonProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setLessonCompleted(
        lesson: Lesson,
        completed: Boolean
    ): Result<Unit> {
        var path = "users/unknown/lessonProgress/${lesson.id}"
        return try {
            val uid = requireUserId()
            require(VALID_ID_REGEX.matches(lesson.id)) { "معرف الدرس غير صالح." }
            require(VALID_ID_REGEX.matches(lesson.subjectId)) { "معرف المادة غير صالح." }
            path = "users/$uid/lessonProgress/${lesson.id}"
            val docRef = db.collection("users").document(uid)
                .collection("lessonProgress").document(lesson.id)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "lessonTitle" to lesson.title.take(140),
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "subjectId" to lesson.subjectId,
                        "lessonId" to lesson.id,
                        "lessonTitle" to lesson.title.take(140),
                        "completed" to completed,
                        "completedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeExamResults(userId: String): Flow<List<ExamResult>> = flow {
        val path = "users/$userId/examResults"
        emitAll(
            db.collection("users").document(userId).collection("examResults")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ExamResult::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.submittedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveExamResult(
        exam: Exam,
        score: Int,
        totalQuestions: Int
    ): Result<ExamResult> {
        var path = "users/unknown/examResults"
        return try {
            val uid = requireUserId()
            val safeTotal = totalQuestions.coerceAtLeast(1)
            val safeScore = score.coerceIn(0, safeTotal)
            val percentage = ((safeScore * 100.0) / safeTotal).toInt().coerceIn(0, 100)
            val passed = percentage >= exam.passingScorePercent
            val resultId = safeDocId("res")
            path = "users/$uid/examResults/$resultId"

            val payload = mapOf(
                "examId" to exam.id,
                "examTitle" to exam.title.take(140),
                "subjectId" to exam.subjectId,
                "subjectName" to exam.subjectName.take(100),
                "score" to safeScore.toLong(),
                "totalQuestions" to safeTotal.toLong(),
                "percentage" to percentage.toLong(),
                "passed" to passed,
                "status" to "completed",
                "submittedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("examResults").document(resultId)
                .set(payload).await()

            Result.success(
                ExamResult(
                    id = resultId,
                    examId = exam.id,
                    examTitle = exam.title,
                    subjectId = exam.subjectId,
                    subjectName = exam.subjectName,
                    score = safeScore.toLong(),
                    totalQuestions = safeTotal.toLong(),
                    percentage = percentage.toLong(),
                    passed = passed,
                    status = "completed"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN CRUD: SUBJECTS ---
    suspend fun upsertSubject(subject: Subject, isEdit: Boolean): Result<Unit> {
        val name = subject.name.trim()
        val code = subject.code.trim()
        val desc = subject.description.trim()
        val instructor = subject.instructor.trim()
        if (name.length !in 2..100) return Result.failure(IllegalArgumentException("اسم المادة يجب أن يكون بين حرفين و100 حرف."))
        if (code.length !in 2..24) return Result.failure(IllegalArgumentException("كود المادة يجب أن يكون بين حرفين و24 حرفاً."))
        if (desc.length !in 2..500) return Result.failure(IllegalArgumentException("وصف المادة مطلوب (2 إلى 500 حرف)."))
        if (instructor.length !in 2..100) return Result.failure(IllegalArgumentException("اسم المحاضر مطلوب (2 إلى 100 حرف)."))

        var path = "subjects"
        return try {
            val uid = requireUserId()
            val docId = safeDocId("subj", subject.id.takeIf { it.isNotBlank() })
            path = "subjects/$docId"
            val docRef = db.collection("subjects").document(docId)
            val exists = isEdit && docRef.get().await().exists()

            if (exists) {
                docRef.update(
                    mapOf(
                        "name" to name,
                        "code" to code,
                        "description" to desc,
                        "instructor" to instructor,
                        "iconKey" to subject.iconKey.ifBlank { "terrain" }.take(40),
                        "orderIndex" to subject.orderIndex.coerceIn(0L, 500L),
                        "published" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "name" to name,
                        "code" to code,
                        "description" to desc,
                        "instructor" to instructor,
                        "iconKey" to subject.iconKey.ifBlank { "terrain" }.take(40),
                        "orderIndex" to subject.orderIndex.coerceIn(0L, 500L),
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val path = "subjects/$subjectId"
        return try {
            requireUserId()
            require(VALID_ID_REGEX.matches(subjectId)) { "معرف المادة غير صالح." }
            db.collection("subjects").document(subjectId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN CRUD: LESSONS (TEXT, IMAGE, VIDEO, PDF) ---
    suspend fun upsertLesson(lesson: Lesson, isEdit: Boolean): Result<Unit> {
        val title = lesson.title.trim()
        val summary = lesson.summary.trim()
        val content = lesson.contentText.trim()
        if (!VALID_ID_REGEX.matches(lesson.subjectId)) return Result.failure(IllegalArgumentException("يرجى اختيار المادة الدراسية."))
        if (title.length !in 2..140) return Result.failure(IllegalArgumentException("عنوان الدرس يجب أن يكون بين حرفين و140 حرفاً."))
        if (summary.length !in 2..400) return Result.failure(IllegalArgumentException("ملخص الدرس مطلوب (2 إلى 400 حرف)."))
        if (content.length !in 5..15000) return Result.failure(IllegalArgumentException("الشرح النصي للدرس مطلوب (5 أحرف على الأقل)."))

        var path = "lessons"
        return try {
            val uid = requireUserId()
            val docId = safeDocId("les", lesson.id.takeIf { it.isNotBlank() })
            path = "lessons/$docId"
            val docRef = db.collection("lessons").document(docId)
            val exists = isEdit && docRef.get().await().exists()

            val imageKey = lesson.imageKey?.trim()?.takeIf { it.isNotEmpty() }?.take(500)
            val videoTitle = lesson.videoTitle?.trim()?.takeIf { it.isNotEmpty() }?.take(140)
            val videoUrl = lesson.videoUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500)
            val pdfTitle = lesson.pdfTitle?.trim()?.takeIf { it.isNotEmpty() }?.take(140)
            val pdfUrl = lesson.pdfUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500)

            if (exists) {
                val updateMap = mutableMapOf<String, Any?>(
                    "subjectId" to lesson.subjectId,
                    "title" to title,
                    "summary" to summary,
                    "contentText" to content,
                    "imageKey" to imageKey,
                    "videoTitle" to videoTitle,
                    "videoUrl" to videoUrl,
                    "pdfTitle" to pdfTitle,
                    "pdfUrl" to pdfUrl,
                    "durationMinutes" to lesson.durationMinutes.coerceIn(1L, 600L),
                    "orderIndex" to lesson.orderIndex.coerceIn(0L, 500L),
                    "published" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "subjectId" to lesson.subjectId,
                    "title" to title,
                    "summary" to summary,
                    "contentText" to content,
                    "imageKey" to imageKey,
                    "videoTitle" to videoTitle,
                    "videoUrl" to videoUrl,
                    "pdfTitle" to pdfTitle,
                    "pdfUrl" to pdfUrl,
                    "durationMinutes" to lesson.durationMinutes.coerceIn(1L, 600L),
                    "orderIndex" to lesson.orderIndex.coerceIn(0L, 500L),
                    "published" to true,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLesson(lessonId: String): Result<Unit> {
        val path = "lessons/$lessonId"
        return try {
            requireUserId()
            require(VALID_ID_REGEX.matches(lessonId)) { "معرف الدرس غير صالح." }
            db.collection("lessons").document(lessonId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN CRUD: EXAMS ---
    suspend fun upsertExam(exam: Exam, isEdit: Boolean): Result<Unit> {
        val title = exam.title.trim()
        val subjectName = exam.subjectName.trim()
        if (!VALID_ID_REGEX.matches(exam.subjectId)) return Result.failure(IllegalArgumentException("يرجى اختيار المادة الدراسية."))
        if (title.length !in 2..140) return Result.failure(IllegalArgumentException("عنوان الامتحان يجب أن يكون بين حرفين و140 حرفاً."))
        if (exam.questions.isEmpty() || exam.questions.size > 30) {
            return Result.failure(IllegalArgumentException("يجب أن يحتوي الامتحان على سؤال واحد على الأقل (وحتى 30 سؤالاً)."))
        }

        val serializedQuestions = exam.questions.map { q ->
            mapOf(
                "questionText" to q.questionText.trim(),
                "options" to q.options.map { it.trim() },
                "correctIndex" to q.correctIndex.coerceIn(0L, (q.options.size - 1).coerceAtLeast(0).toLong()),
                "explanation" to q.explanation.trim()
            )
        }

        var path = "exams"
        return try {
            val uid = requireUserId()
            val docId = safeDocId("exam", exam.id.takeIf { it.isNotBlank() })
            path = "exams/$docId"
            val docRef = db.collection("exams").document(docId)
            val exists = isEdit && docRef.get().await().exists()
            val desc = exam.description?.trim()?.takeIf { it.isNotEmpty() }?.take(400)

            if (exists) {
                val updateMap = mutableMapOf<String, Any?>(
                    "subjectId" to exam.subjectId,
                    "subjectName" to subjectName.take(100),
                    "title" to title,
                    "description" to desc,
                    "durationMinutes" to exam.durationMinutes.coerceIn(1L, 240L),
                    "passingScorePercent" to exam.passingScorePercent.coerceIn(1L, 100L),
                    "questions" to serializedQuestions,
                    "published" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "subjectId" to exam.subjectId,
                    "subjectName" to subjectName.take(100),
                    "title" to title,
                    "description" to desc,
                    "durationMinutes" to exam.durationMinutes.coerceIn(1L, 240L),
                    "passingScorePercent" to exam.passingScorePercent.coerceIn(1L, 100L),
                    "questions" to serializedQuestions,
                    "published" to true,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteExam(examId: String): Result<Unit> {
        val path = "exams/$examId"
        return try {
            requireUserId()
            require(VALID_ID_REGEX.matches(examId)) { "معرف الامتحان غير صالح." }
            db.collection("exams").document(examId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Seeds all 8 First-Year Surveying subjects, 16 lessons, and 8 MCQ exams into Cloud Firestore.
     * Writes subjects first so exists(/subjects/$(subjectId)) rules pass for lessons & exams.
     */
    suspend fun seedDefaultCurriculum(forceOverwrite: Boolean = false): Result<Int> {
        return try {
            requireUserId()
            if (!forceOverwrite) {
                val existingSubjects = db.collection("subjects")
                    .whereEqualTo("published", true)
                    .limit(1)
                    .get()
                    .await()
                if (!existingSubjects.isEmpty) {
                    return Result.success(0)
                }
            }

            var writtenCount = 0
            // 1. Seed subjects first (parent documents)
            for (subject in DefaultCurriculum.subjects) {
                val res = upsertSubject(subject, isEdit = true)
                if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)
                writtenCount++
            }
            // 2. Seed lessons referencing those subjects
            for (lesson in DefaultCurriculum.lessons) {
                val res = upsertLesson(lesson, isEdit = true)
                if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)
                writtenCount++
            }
            // 3. Seed exams referencing those subjects
            for (exam in DefaultCurriculum.exams) {
                val res = upsertExam(exam, isEdit = true)
                if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)
                writtenCount++
            }
            Result.success(writtenCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "subjects")
            Result.failure(e)
        }
    }
}
