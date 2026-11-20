package com.example.data.local

import android.content.Context
import com.example.data.model.DefaultCurriculum
import com.example.data.model.Exam
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamResult
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Durable on-device persistence for immediate no-login access to Mesaha Academy.
 * Allows students and admins to browse all 8 subjects, complete lessons, take exams,
 * and manage content without hitting Firestore while unauthenticated.
 */
class LocalAcademyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadProfile(): UserProfile {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return defaultProfile()
        return try {
            val obj = JSONObject(raw)
            UserProfile(
                id = "local_student",
                userId = "local_student",
                fullName = obj.optString("fullName", "طالب الفرقة الأولى"),
                email = obj.optString("email", ""),
                studentCode = obj.optString("studentCode", "2026-101"),
                department = obj.optString("department", "الفرقة الأولى - معهد المساحة"),
                phone = obj.optString("phone", "").ifBlank { null }
            )
        } catch (_: Exception) {
            defaultProfile()
        }
    }

    fun saveProfile(profile: UserProfile) {
        val obj = JSONObject().apply {
            put("fullName", profile.fullName)
            put("email", profile.email)
            put("studentCode", profile.studentCode)
            put("department", profile.department)
            put("phone", profile.phone.orEmpty())
        }
        prefs.edit().putString(KEY_PROFILE, obj.toString()).apply()
    }

    fun loadSubjects(): List<Subject> {
        val raw = prefs.getString(KEY_SUBJECTS, null) ?: return DefaultCurriculum.subjects
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Subject(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            code = o.optString("code"),
                            description = o.optString("description"),
                            instructor = o.optString("instructor"),
                            iconKey = o.optString("iconKey", "terrain"),
                            orderIndex = o.optLong("orderIndex", i.toLong()),
                            published = true
                        )
                    )
                }
            }.sortedBy { it.orderIndex }
        } catch (_: Exception) {
            DefaultCurriculum.subjects
        }
    }

    fun saveSubjects(subjects: List<Subject>) {
        val arr = JSONArray()
        subjects.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("code", s.code)
                    put("description", s.description)
                    put("instructor", s.instructor)
                    put("iconKey", s.iconKey)
                    put("orderIndex", s.orderIndex)
                }
            )
        }
        prefs.edit().putString(KEY_SUBJECTS, arr.toString()).apply()
    }

    fun loadLessons(): List<Lesson> {
        val raw = prefs.getString(KEY_LESSONS, null) ?: return DefaultCurriculum.lessons
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Lesson(
                            id = o.optString("id"),
                            subjectId = o.optString("subjectId"),
                            title = o.optString("title"),
                            summary = o.optString("summary"),
                            contentText = o.optString("contentText"),
                            imageKey = o.optString("imageKey", "").ifBlank { null },
                            videoTitle = o.optString("videoTitle", "").ifBlank { null },
                            videoUrl = o.optString("videoUrl", "").ifBlank { null },
                            pdfTitle = o.optString("pdfTitle", "").ifBlank { null },
                            pdfUrl = o.optString("pdfUrl", "").ifBlank { null },
                            durationMinutes = o.optLong("durationMinutes", 25L),
                            orderIndex = o.optLong("orderIndex", i.toLong()),
                            published = true
                        )
                    )
                }
            }.sortedBy { it.orderIndex }
        } catch (_: Exception) {
            DefaultCurriculum.lessons
        }
    }

    fun saveLessons(lessons: List<Lesson>) {
        val arr = JSONArray()
        lessons.forEach { l ->
            arr.put(
                JSONObject().apply {
                    put("id", l.id)
                    put("subjectId", l.subjectId)
                    put("title", l.title)
                    put("summary", l.summary)
                    put("contentText", l.contentText)
                    put("imageKey", l.imageKey.orEmpty())
                    put("videoTitle", l.videoTitle.orEmpty())
                    put("videoUrl", l.videoUrl.orEmpty())
                    put("pdfTitle", l.pdfTitle.orEmpty())
                    put("pdfUrl", l.pdfUrl.orEmpty())
                    put("durationMinutes", l.durationMinutes)
                    put("orderIndex", l.orderIndex)
                }
            )
        }
        prefs.edit().putString(KEY_LESSONS, arr.toString()).apply()
    }

    fun loadExams(): List<Exam> {
        val raw = prefs.getString(KEY_EXAMS, null) ?: return DefaultCurriculum.exams
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val qArr = o.optJSONArray("questions") ?: JSONArray()
                    val questions = buildList {
                        for (qIdx in 0 until qArr.length()) {
                            val qo = qArr.getJSONObject(qIdx)
                            val optArr = qo.optJSONArray("options") ?: JSONArray()
                            val opts = buildList {
                                for (k in 0 until optArr.length()) {
                                    add(optArr.optString(k))
                                }
                            }
                            add(
                                ExamQuestion(
                                    questionText = qo.optString("questionText"),
                                    options = opts,
                                    correctIndex = qo.optLong("correctIndex", 0L),
                                    explanation = qo.optString("explanation")
                                )
                            )
                        }
                    }
                    add(
                        Exam(
                            id = o.optString("id"),
                            subjectId = o.optString("subjectId"),
                            subjectName = o.optString("subjectName"),
                            title = o.optString("title"),
                            description = o.optString("description", "").ifBlank { null },
                            durationMinutes = o.optLong("durationMinutes", 10L),
                            passingScorePercent = o.optLong("passingScorePercent", 60L),
                            questions = questions,
                            published = true
                        )
                    )
                }
            }
        } catch (_: Exception) {
            DefaultCurriculum.exams
        }
    }

    fun saveExams(exams: List<Exam>) {
        val arr = JSONArray()
        exams.forEach { e ->
            val qArr = JSONArray()
            e.questions.forEach { q ->
                val optArr = JSONArray()
                q.options.forEach { optArr.put(it) }
                qArr.put(
                    JSONObject().apply {
                        put("questionText", q.questionText)
                        put("options", optArr)
                        put("correctIndex", q.correctIndex)
                        put("explanation", q.explanation)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("subjectId", e.subjectId)
                    put("subjectName", e.subjectName)
                    put("title", e.title)
                    put("description", e.description.orEmpty())
                    put("durationMinutes", e.durationMinutes)
                    put("passingScorePercent", e.passingScorePercent)
                    put("questions", qArr)
                }
            )
        }
        prefs.edit().putString(KEY_EXAMS, arr.toString()).apply()
    }

    fun loadCompletedLessonIds(): Set<String> {
        return prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet())?.toSet() ?: emptySet()
    }

    fun saveCompletedLessonIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_COMPLETED_LESSONS, ids.toSet()).apply()
    }

    fun loadExamResults(): List<ExamResult> {
        val raw = prefs.getString(KEY_EXAM_RESULTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val millis = o.optLong("submittedAtMillis", System.currentTimeMillis())
                    add(
                        ExamResult(
                            id = o.optString("id"),
                            examId = o.optString("examId"),
                            examTitle = o.optString("examTitle"),
                            subjectId = o.optString("subjectId"),
                            subjectName = o.optString("subjectName"),
                            score = o.optLong("score", 0L),
                            totalQuestions = o.optLong("totalQuestions", 1L),
                            percentage = o.optLong("percentage", 0L),
                            passed = o.optBoolean("passed", false),
                            status = "completed",
                            submittedAt = Timestamp(Date(millis))
                        )
                    )
                }
            }.sortedByDescending { it.submittedAt?.seconds ?: 0L }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveExamResults(results: List<ExamResult>) {
        val arr = JSONArray()
        results.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("examId", r.examId)
                    put("examTitle", r.examTitle)
                    put("subjectId", r.subjectId)
                    put("subjectName", r.subjectName)
                    put("score", r.score)
                    put("totalQuestions", r.totalQuestions)
                    put("percentage", r.percentage)
                    put("passed", r.passed)
                    put("submittedAtMillis", r.submittedAt?.toDate()?.time ?: System.currentTimeMillis())
                }
            )
        }
        prefs.edit().putString(KEY_EXAM_RESULTS, arr.toString()).apply()
    }

    fun resetCurriculumToDefaults() {
        saveSubjects(DefaultCurriculum.subjects)
        saveLessons(DefaultCurriculum.lessons)
        saveExams(DefaultCurriculum.exams)
    }

    private fun defaultProfile() = UserProfile(
        id = "local_student",
        userId = "local_student",
        fullName = "طالب الفرقة الأولى",
        email = "",
        studentCode = "2026-101",
        department = "الفرقة الأولى - معهد المساحة"
    )

    private companion object {
        const val PREFS_NAME = "mesaha_local_academy_store"
        const val KEY_PROFILE = "profile_json"
        const val KEY_SUBJECTS = "subjects_json"
        const val KEY_LESSONS = "lessons_json"
        const val KEY_EXAMS = "exams_json"
        const val KEY_COMPLETED_LESSONS = "completed_lessons_set"
        const val KEY_EXAM_RESULTS = "exam_results_json"
    }
}
