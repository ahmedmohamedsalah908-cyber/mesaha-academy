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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.ExamResult
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AcademyDashboardState
import kotlinx.coroutines.delay

@Composable
fun ExamsScreen(
    state: AcademyDashboardState,
    onStartExam: (String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    val filteredExams = remember(state.exams, selectedSubjectId) {
        if (selectedSubjectId == null) state.exams
        else state.exams.filter { it.subjectId == selectedSubjectId }
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
                    title = "قسم الامتحانات والاختبارات",
                    subtitle = "اختبارات اختيار من متعدد مع تصحيح تلقائي وحفظ النتيجة"
                )
            }

            // Subject Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedSubjectId == null,
                            onClick = { selectedSubjectId = null },
                            label = { Text("جميع المواد") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDark,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(state.subjects, key = { it.id }) { subject ->
                        FilterChip(
                            selected = selectedSubjectId == subject.id,
                            onClick = {
                                selectedSubjectId = if (selectedSubjectId == subject.id) null else subject.id
                            },
                            label = { Text(subject.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDark,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filteredExams.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Quiz,
                        title = "لا توجد امتحانات لهذه المادة حالياً",
                        subtitle = "اختر مادة أخرى أو أضف امتحاناً جديداً من لوحة تحكم المدير."
                    )
                }
            } else {
                items(filteredExams, key = { it.id }) { exam ->
                    val bestResult = state.bestExamResult(exam.id)
                    ExamOverviewCard(
                        exam = exam,
                        bestResult = bestResult,
                        onStart = { onStartExam(exam.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ExamOverviewCard(
    exam: Exam,
    bestResult: ExamResult?,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = exam.subjectName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (bestResult != null) {
                    Surface(
                        color = if (bestResult.passed) SuccessContainer else ErrorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "أعلى نتيجة: ${bestResult.percentage}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (bestResult.passed) SuccessGreen else ErrorCrimson,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = exam.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!exam.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exam.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Quiz, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${exam.questions.size} أسئلة", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${exam.durationMinutes} دقيقة", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("النجاح ${exam.passingScorePercent}%", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_exam_button_${exam.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text(
                    text = if (bestResult != null) "إعادة الامتحان لتحسين الدرجة" else "ابدأ الامتحان الآن",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ExamTakingScreen(
    exam: Exam?,
    onBack: () -> Unit,
    onSubmitResult: (score: Int, total: Int, onSaved: (ExamResult) -> Unit) -> Unit
) {
    if (exam == null || exam.questions.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            EmptyStateCard(
                icon = Icons.Filled.Quiz,
                title = "الامتحان غير متوفر",
                subtitle = "لا توجد أسئلة مضافة لهذا الامتحان حالياً."
            )
        }
        return
    }

    var currentIndex by remember(exam.id) { mutableIntStateOf(0) }
    val selectedAnswers = remember(exam.id) { mutableStateMapOf<Int, Int>() }
    var submittedResult by remember(exam.id) { mutableStateOf<ExamResult?>(null) }
    var remainingSeconds by remember(exam.id) { mutableIntStateOf((exam.durationMinutes * 60).toInt()) }

    fun finishAndGrade() {
        if (submittedResult != null) return
        var score = 0
        exam.questions.forEachIndexed { idx, question ->
            if (selectedAnswers[idx] == question.correctIndex.toInt()) {
                score++
            }
        }
        onSubmitResult(score, exam.questions.size) { saved ->
            submittedResult = saved
        }
    }

    // Live countdown timer while taking exam
    LaunchedEffect(exam.id, submittedResult) {
        if (submittedResult == null) {
            while (remainingSeconds > 0 && submittedResult == null) {
                delay(1000L)
                remainingSeconds--
            }
            if (remainingSeconds == 0 && submittedResult == null) {
                finishAndGrade()
            }
        }
    }

    val result = submittedResult
    if (result != null) {
        ExamResultReviewView(
            exam = exam,
            result = result,
            selectedAnswers = selectedAnswers,
            onRetake = {
                selectedAnswers.clear()
                currentIndex = 0
                remainingSeconds = (exam.durationMinutes * 60).toInt()
                submittedResult = null
            },
            onDone = onBack
        )
        return
    }

    val question = exam.questions[currentIndex]
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val progress = (currentIndex + 1).toFloat() / exam.questions.size.toFloat()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar with timer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("exam_exit_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "خروج من الامتحان")
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = exam.subjectName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "السؤال ${currentIndex + 1} من ${exam.questions.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = if (remainingSeconds < 60) ErrorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "الوقت المتبقي",
                                tint = if (remainingSeconds < 60) ErrorCrimson else EmeraldDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingSeconds < 60) ErrorCrimson else EmeraldDark
                            )
                        }
                    }
                }
            }

            item {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = EmeraldPrimary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = StrokeCap.Round
                )
            }

            // Question Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark)
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "السؤال رقم ${currentIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Answer Choices
            items(question.options.size) { optionIdx ->
                val optionText = question.options[optionIdx]
                val isSelected = selectedAnswers[currentIndex] == optionIdx
                val arabicLetter = listOf("أ", "ب", "ج", "د", "هـ", "و").getOrElse(optionIdx) { "${optionIdx + 1}" }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exam_option_$optionIdx")
                        .clickable { selectedAnswers[currentIndex] = optionIdx },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = arabicLetter,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Navigation Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = { currentIndex-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("exam_previous_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("السؤال السابق", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (currentIndex < exam.questions.lastIndex) {
                        Button(
                            onClick = { currentIndex++ },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("exam_next_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                        ) {
                            Text("السؤال التالي", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { finishAndGrade() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("exam_submit_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text(
                                text = "إنهاء الامتحان والتصحيح (${selectedAnswers.size}/${exam.questions.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun ExamResultReviewView(
    exam: Exam,
    result: ExamResult,
    selectedAnswers: Map<Int, Int>,
    onRetake: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    if (result.passed) listOf(EmeraldDark, SuccessGreen)
                                    else listOf(Color(0xFF7B1B20), ErrorCrimson)
                                )
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (result.passed) Icons.Filled.EmojiEvents else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (result.passed) "مبارك! اجتزت الامتحان بنجاح" else "لم تحقق درجة النجاح بعد — حاول مرة أخرى",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${result.percentage}%",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Text(
                                text = "الإجابات الصحيحة: ${result.score} من ${result.totalQuestions}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onDone,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("exam_result_done_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = EmeraldDark
                                    )
                                ) {
                                    Text("العودة للامتحانات", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onRetake,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("exam_result_retake_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة")
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeaderRow(
                    title = "مراجعة الإجابات والتصحيح التلقائي",
                    subtitle = "تعرف على الإجابة النموذجية وشرح كل سؤال"
                )
            }

            items(exam.questions.size) { idx ->
                val q = exam.questions[idx]
                val userChoice = selectedAnswers[idx]
                val correctIdx = q.correctIndex.toInt()
                val isCorrect = userChoice == correctIdx

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, if (isCorrect) SuccessGreen.copy(alpha = 0.5f) else ErrorCrimson.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = if (isCorrect) SuccessGreen else ErrorCrimson,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${idx + 1}. ${q.questionText}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val userText = userChoice?.let { q.options.getOrNull(it) } ?: "لم تتم الإجابة"
                        Text(
                            text = "إجابتك: $userText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCorrect) SuccessGreen else ErrorCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isCorrect) {
                            Text(
                                text = "الإجابة الصحيحة: ${q.options.getOrNull(correctIdx).orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (q.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "التوضيح: ${q.explanation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
