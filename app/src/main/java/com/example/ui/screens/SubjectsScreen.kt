package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Exam
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SubjectProgressCard
import com.example.ui.components.subjectIconForKey
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AcademyDashboardState

@Composable
fun SubjectsScreen(
    state: AcademyDashboardState,
    onOpenSubject: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSubjects = remember(state.subjects, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) state.subjects
        else state.subjects.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.code.contains(q, ignoreCase = true) ||
                it.instructor.contains(q, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeaderRow(
                    title = "المواد الدراسية",
                    subtitle = "الفرقة الأولى - معهد المساحة (${state.subjects.size} مواد)"
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث باسم المادة أو المحاضر...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subjects_search_input")
                )
            }

            if (filteredSubjects.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.MenuBook,
                        title = "لا توجد مواد مطابقة",
                        subtitle = "جرب البحث بكلمة مختلفة أو امسح مربع البحث لعرض جميع المواد."
                    )
                }
            } else {
                items(filteredSubjects, key = { it.id }) { subject ->
                    val subjLessons = state.lessonsForSubject(subject.id)
                    val doneCount = subjLessons.count { it.id in state.completedLessonIds }
                    SubjectProgressCard(
                        subject = subject,
                        lessonsCount = subjLessons.size,
                        completedCount = doneCount,
                        onClick = { onOpenSubject(subject.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun SubjectDetailScreen(
    subject: Subject?,
    lessons: List<Lesson>,
    exams: List<Exam>,
    completedLessonIds: Set<String>,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onStartExam: (String) -> Unit
) {
    if (subject == null) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            EmptyStateCard(
                icon = Icons.Filled.MenuBook,
                title = "المادة غير متوفرة",
                subtitle = "ربما تم حذف هذه المادة، يرجى العودة لقائمة المواد."
            )
        }
        return
    }

    val doneCount = lessons.count { it.id in completedLessonIds }
    val progress = if (lessons.isEmpty()) 0f else doneCount.toFloat() / lessons.size.toFloat()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Emerald Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(EmeraldDark, EmeraldPrimary)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier.testTag("subject_detail_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "العودة",
                                            tint = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject.code,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldAccent
                                    )
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = subjectIconForKey(subject.iconKey),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "المحاضر: ${subject.instructor}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = subject.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "نسبة إنجاز المادة ($doneCount / ${lessons.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = GoldAccent,
                                trackColor = Color.White.copy(alpha = 0.25f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Lessons Section
            item {
                SectionHeaderRow(
                    title = "قائمة الدروس (${lessons.size})",
                    subtitle = "تتضمن شرحاً نصياً وصوراً وفيديوهات وملفات PDF"
                )
            }

            if (lessons.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Description,
                        title = "لا توجد دروس مضافة بعد",
                        subtitle = "يمكن للمدير إضافة دروس جديدة من لوحة التحكم."
                    )
                }
            } else {
                items(lessons, key = { it.id }) { lesson ->
                    val isDone = lesson.id in completedLessonIds
                    LessonListItemCard(
                        lesson = lesson,
                        isCompleted = isDone,
                        onClick = { onOpenLesson(lesson.id) }
                    )
                }
            }

            // Exams Section for this subject
            if (exams.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeaderRow(
                        title = "امتحانات المادة (${exams.size})",
                        subtitle = "اختبر فهمك واحصل على التصحيح التلقائي فوراً"
                    )
                }

                items(exams, key = { it.id }) { exam ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Quiz,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${exam.questions.size} أسئلة • ${exam.durationMinutes} دقيقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onStartExam(exam.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                modifier = Modifier.testTag("start_subject_exam_${exam.id}")
                            ) {
                                Text("ابدأ")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LessonListItemCard(
    lesson: Lesson,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lesson_card_${lesson.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = if (isCompleted) 1.5.dp else 1.dp,
            color = if (isCompleted) SuccessGreen.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "درس مكتمل" else "درس غير مكتمل",
                    tint = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lesson.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResourceBadge(icon = Icons.Filled.Schedule, label = "${lesson.durationMinutes} د")
                ResourceBadge(icon = Icons.Filled.Image, label = "صور")
                if (!lesson.videoUrl.isNullOrBlank()) {
                    ResourceBadge(icon = Icons.Filled.PlayCircle, label = "فيديو")
                }
                ResourceBadge(icon = Icons.Filled.PictureAsPdf, label = "PDF")
                Spacer(modifier = Modifier.weight(1f))
                if (isCompleted) {
                    Surface(
                        color = SuccessContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "مكتمل",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
