package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Exam
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.AcademyDashboardState

@Composable
fun AdminDashboardScreen(
    state: AcademyDashboardState,
    onSeedCurriculum: () -> Unit,
    onSaveSubject: (Subject, Boolean) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onSaveLesson: (Lesson, Boolean) -> Unit,
    onDeleteLesson: (String) -> Unit,
    onSaveExam: (Exam, Boolean) -> Unit,
    onDeleteExam: (String) -> Unit
) {
    if (!state.isAdmin) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Filled.Lock,
                title = "صلاحيات المدير مطلوبة",
                subtitle = "هذه اللوحة مخصصة لمدير أكاديمية المساحة فقط لإضافة وتعديل وحذف المواد والدروس والامتحانات."
            )
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingSubject by remember { mutableStateOf<Pair<Subject?, Boolean>?>(null) }
    var editingLesson by remember { mutableStateOf<Pair<Lesson?, Boolean>?>(null) }
    var editingExam by remember { mutableStateOf<Pair<Exam?, Boolean>?>(null) }
    var pendingDelete by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    val subjectMap = remember(state.subjects) { state.subjects.associateBy { it.id } }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
        ) {
            // Admin Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(EmeraldDark, EmeraldPrimary)))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "لوحة تحكم المدير",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "إدارة كاملة للمواد والدروس والفيديوهات والامتحانات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            if (state.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = GoldAccent,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    when (selectedTab) {
                                        0 -> editingSubject = Pair(null, false)
                                        1 -> editingLesson = Pair(null, false)
                                        else -> editingExam = Pair(null, false)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_add_item_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = EmeraldDark
                                )
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "إضافة مادة"
                                        1 -> "إضافة درس/فيديو"
                                        else -> "إضافة امتحان"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onSeedCurriculum,
                                enabled = !state.isBusy,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("admin_seed_curriculum_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = EmeraldDark
                                )
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تهيئة المنهج", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tabs: Subjects | Lessons & Videos | Exams
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldDark
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المواد (${state.subjects.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_subjects")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الدروس والفيديوهات (${state.lessons.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_lessons")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("الامتحانات (${state.exams.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_exams")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (state.subjects.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.MenuBook,
                                    title = "لا توجد مواد مسجلة",
                                    subtitle = "اضغط على «تهيئة المنهج» أو «إضافة مادة» لبدء إدارة المحتوى."
                                )
                            }
                        } else {
                            items(state.subjects, key = { it.id }) { subject ->
                                AdminManageRowCard(
                                    title = subject.name,
                                    subtitle = "${subject.code} • ${subject.instructor}",
                                    badge = "${state.lessonsForSubject(subject.id).size} دروس",
                                    onEdit = { editingSubject = Pair(subject, true) },
                                    onDelete = {
                                        pendingDelete = Triple(
                                            "حذف المادة الدراسية",
                                            "هل أنت متأكد من حذف مادة «${subject.name}» من قاعدة البيانات؟"
                                        ) { onDeleteSubject(subject.id) }
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        if (state.lessons.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.PlayCircle,
                                    title = "لا توجد دروس مسجلة",
                                    subtitle = "أضف درساً جديداً يتضمن شرحاً نصياً وصورة ورابط فيديو وملف PDF."
                                )
                            }
                        } else {
                            items(state.lessons, key = { it.id }) { lesson ->
                                val subjName = subjectMap[lesson.subjectId]?.name ?: lesson.subjectId
                                AdminManageRowCard(
                                    title = lesson.title,
                                    subtitle = "$subjName • ${lesson.durationMinutes} دقيقة",
                                    badge = if (!lesson.videoUrl.isNullOrBlank()) "فيديو + PDF" else "نص + PDF",
                                    onEdit = { editingLesson = Pair(lesson, true) },
                                    onDelete = {
                                        pendingDelete = Triple(
                                            "حذف الدرس",
                                            "هل أنت متأكد من حذف درس «${lesson.title}»؟"
                                        ) { onDeleteLesson(lesson.id) }
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        if (state.exams.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.Quiz,
                                    title = "لا توجد امتحانات مسجلة",
                                    subtitle = "اضغط على «إضافة امتحان» لإنشاء اختبار اختيار من متعدد."
                                )
                            }
                        } else {
                            items(state.exams, key = { it.id }) { exam ->
                                AdminManageRowCard(
                                    title = exam.title,
                                    subtitle = "${exam.subjectName} • ${exam.durationMinutes} دقيقة • النجاح ${exam.passingScorePercent}%",
                                    badge = "${exam.questions.size} أسئلة",
                                    onEdit = { editingExam = Pair(exam, true) },
                                    onDelete = {
                                        pendingDelete = Triple(
                                            "حذف الامتحان",
                                            "هل أنت متأكد من حذف امتحان «${exam.title}»؟"
                                        ) { onDeleteExam(exam.id) }
                                    }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Editor Dialogs
    editingSubject?.let { (initial, _) ->
        SubjectEditorDialog(
            initial = initial,
            nextOrderIndex = (state.subjects.size + 1).toLong(),
            onDismiss = { editingSubject = null },
            onSave = { subj, isEdit ->
                onSaveSubject(subj, isEdit)
                editingSubject = null
            }
        )
    }

    editingLesson?.let { (initial, _) ->
        LessonEditorDialog(
            initial = initial,
            subjects = state.subjects,
            onDismiss = { editingLesson = null },
            onSave = { lesson, isEdit ->
                onSaveLesson(lesson, isEdit)
                editingLesson = null
            }
        )
    }

    editingExam?.let { (initial, _) ->
        ExamEditorDialog(
            initial = initial,
            subjects = state.subjects,
            onDismiss = { editingExam = null },
            onSave = { exam, isEdit ->
                onSaveExam(exam, isEdit)
                editingExam = null
            }
        )
    }

    pendingDelete?.let { (title, message, confirmAction) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction()
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson)
                ) {
                    Text("حذف نهائياً")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun AdminManageRowCard(
    title: String,
    subtitle: String,
    badge: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = EmeraldPrimary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = ErrorCrimson)
            }
        }
    }
}
