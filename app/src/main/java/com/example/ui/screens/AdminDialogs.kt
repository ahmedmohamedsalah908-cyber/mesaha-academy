package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Exam
import com.example.data.model.ExamQuestion
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.ErrorCrimson

private val ICON_CHOICES = listOf(
    "terrain" to "مساحة",
    "architecture" to "تطبيقات",
    "calculate" to "رياضيات",
    "translate" to "إنجليزية",
    "precision" to "أجهزة",
    "draw" to "رسم فني",
    "health" to "سلامة",
    "computer" to "حاسب"
)

private val IMAGE_PRESETS = listOf(
    "plane_surveying" to "رسم المساحة المستوية",
    "instruments" to "رسم الأجهزة المساحية",
    "hero_surveying" to "خلفية كنتورية"
)

@Composable
fun SubjectEditorDialog(
    initial: Subject?,
    nextOrderIndex: Long,
    onDismiss: () -> Unit,
    onSave: (Subject, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var code by remember { mutableStateOf(initial?.code.orEmpty()) }
    var instructor by remember { mutableStateOf(initial?.instructor.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var iconKey by remember { mutableStateOf(initial?.iconKey ?: "terrain") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة مادة دراسية جديدة" else "تعديل المادة الدراسية", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المادة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود المادة (مثال: SURV-109) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("اسم المحاضر *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف مختصر للمادة *") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("اختر أيقونة المادة:", style = MaterialTheme.typography.labelMedium)
                ICON_CHOICES.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowItems.forEach { (key, label) ->
                            FilterChip(
                                selected = iconKey == key,
                                onClick = { iconKey = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val model = (initial ?: Subject()).copy(
                        name = name.trim(),
                        code = code.trim(),
                        instructor = instructor.trim(),
                        description = description.trim(),
                        iconKey = iconKey,
                        orderIndex = initial?.orderIndex ?: nextOrderIndex,
                        published = true
                    )
                    onSave(model, initial != null)
                },
                enabled = name.trim().length >= 2 && code.trim().length >= 2 && instructor.trim().length >= 2 && description.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun LessonEditorDialog(
    initial: Lesson?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (Lesson, Boolean) -> Unit
) {
    var selectedSubjectId by remember {
        mutableStateOf(initial?.subjectId?.takeIf { id -> subjects.any { it.id == id } } ?: subjects.firstOrNull()?.id.orEmpty())
    }
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var summary by remember { mutableStateOf(initial?.summary.orEmpty()) }
    var contentText by remember { mutableStateOf(initial?.contentText.orEmpty()) }
    var imageKey by remember { mutableStateOf(initial?.imageKey ?: "plane_surveying") }
    var videoTitle by remember { mutableStateOf(initial?.videoTitle.orEmpty()) }
    var videoUrl by remember { mutableStateOf(initial?.videoUrl.orEmpty()) }
    var pdfTitle by remember { mutableStateOf(initial?.pdfTitle.orEmpty()) }
    var pdfUrl by remember { mutableStateOf(initial?.pdfUrl.orEmpty()) }
    var durationText by remember { mutableStateOf((initial?.durationMinutes ?: 25L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة درس وفيديو جديد" else "تعديل الدرس والفيديو", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("المادة الدراسية:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                subjects.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { subj ->
                            FilterChip(
                                selected = selectedSubjectId == subj.id,
                                onClick = { selectedSubjectId = subj.id },
                                label = { Text(subj.name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الدرس *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("ملخص قصير *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("الشرح النصي الكامل للدرس *") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit) },
                    label = { Text("مدة الدرس بالدقائق") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    label = { Text("عنوان الفيديو التعليمي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("رابط الفيديو (YouTube أو رابط مباشر)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pdfTitle,
                    onValueChange = { pdfTitle = it },
                    label = { Text("عنوان ملف PDF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pdfUrl,
                    onValueChange = { pdfUrl = it },
                    label = { Text("رابط PDF (اختياري — يُنشأ تلقائياً إذا تُرك فارغاً)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageKey,
                    onValueChange = { imageKey = it },
                    label = { Text("صورة الدرس (أو رابط صورة HTTPS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IMAGE_PRESETS.forEach { (key, label) ->
                        FilterChip(
                            selected = imageKey == key,
                            onClick = { imageKey = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val model = (initial ?: Lesson()).copy(
                        subjectId = selectedSubjectId,
                        title = title.trim(),
                        summary = summary.trim(),
                        contentText = contentText.trim(),
                        imageKey = imageKey.trim().ifBlank { null },
                        videoTitle = videoTitle.trim().ifBlank { null },
                        videoUrl = videoUrl.trim().ifBlank { null },
                        pdfTitle = pdfTitle.trim().ifBlank { null },
                        pdfUrl = pdfUrl.trim().ifBlank { null },
                        durationMinutes = durationText.toLongOrNull()?.coerceIn(1L, 600L) ?: 25L,
                        orderIndex = initial?.orderIndex ?: 1L,
                        published = true
                    )
                    onSave(model, initial != null)
                },
                enabled = selectedSubjectId.isNotBlank() && title.trim().length >= 2 && summary.trim().length >= 2 && contentText.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("حفظ الدرس")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

private data class EditableQuestion(
    var questionText: String = "",
    var optionA: String = "",
    var optionB: String = "",
    var optionC: String = "",
    var optionD: String = "",
    var correctIndex: Int = 0,
    var explanation: String = ""
)

@Composable
fun ExamEditorDialog(
    initial: Exam?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (Exam, Boolean) -> Unit
) {
    var selectedSubjectId by remember {
        mutableStateOf(initial?.subjectId?.takeIf { id -> subjects.any { it.id == id } } ?: subjects.firstOrNull()?.id.orEmpty())
    }
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var durationText by remember { mutableStateOf((initial?.durationMinutes ?: 10L).toString()) }
    var passingText by remember { mutableStateOf((initial?.passingScorePercent ?: 60L).toString()) }

    val questions = remember {
        mutableStateListOf<EditableQuestion>().apply {
            if (initial != null && initial.questions.isNotEmpty()) {
                initial.questions.forEach { q ->
                    add(
                        EditableQuestion(
                            questionText = q.questionText,
                            optionA = q.options.getOrElse(0) { "" },
                            optionB = q.options.getOrElse(1) { "" },
                            optionC = q.options.getOrElse(2) { "" },
                            optionD = q.options.getOrElse(3) { "" },
                            correctIndex = q.correctIndex.toInt().coerceIn(0, 3),
                            explanation = q.explanation
                        )
                    )
                }
            } else {
                add(EditableQuestion())
            }
        }
    }
    var refreshTick by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة امتحان جديد" else "تعديل الامتحان والأسئلة", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("المادة الدراسية:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                subjects.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { subj ->
                            FilterChip(
                                selected = selectedSubjectId == subj.id,
                                onClick = { selectedSubjectId = subj.id },
                                label = { Text(subj.name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الامتحان *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الامتحان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit) },
                        label = { Text("المدة (دقيقة)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passingText,
                        onValueChange = { passingText = it.filter(Char::isDigit) },
                        label = { Text("نسبة النجاح %") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "أسئلة الاختيار من متعدد (${questions.size}) — tick:$refreshTick".substringBefore(" — tick:"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                questions.forEachIndexed { qIdx, eq ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("السؤال ${qIdx + 1}", fontWeight = FontWeight.Bold)
                                if (questions.size > 1) {
                                    IconButton(onClick = { questions.removeAt(qIdx); refreshTick++ }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "حذف السؤال", tint = ErrorCrimson)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = eq.questionText,
                                onValueChange = { questions[qIdx] = eq.copy(questionText = it); refreshTick++ },
                                label = { Text("نص السؤال *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            val opts = listOf(
                                Triple(0, "الاختيار أ *", eq.optionA),
                                Triple(1, "الاختيار ب *", eq.optionB),
                                Triple(2, "الاختيار ج", eq.optionC),
                                Triple(3, "الاختيار د", eq.optionD)
                            )
                            opts.forEach { (optIdx, label, value) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = eq.correctIndex == optIdx,
                                        onClick = { questions[qIdx] = eq.copy(correctIndex = optIdx); refreshTick++ }
                                    )
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { newVal ->
                                            questions[qIdx] = when (optIdx) {
                                                0 -> eq.copy(optionA = newVal)
                                                1 -> eq.copy(optionB = newVal)
                                                2 -> eq.copy(optionC = newVal)
                                                else -> eq.copy(optionD = newVal)
                                            }
                                            refreshTick++
                                        },
                                        label = { Text(label) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = eq.explanation,
                                onValueChange = { questions[qIdx] = eq.copy(explanation = it); refreshTick++ },
                                label = { Text("توضيح الإجابة الصحيحة") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (questions.size < 30) {
                    OutlinedButton(
                        onClick = { questions.add(EditableQuestion()); refreshTick++ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة سؤال جديد")
                    }
                }
            }
        },
        confirmButton = {
            val builtQuestions = questions.mapNotNull { eq ->
                val cleanOpts = listOf(eq.optionA, eq.optionB, eq.optionC, eq.optionD)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (eq.questionText.trim().length >= 2 && cleanOpts.size >= 2) {
                    ExamQuestion(
                        questionText = eq.questionText.trim(),
                        options = cleanOpts,
                        correctIndex = eq.correctIndex.coerceIn(0, cleanOpts.lastIndex).toLong(),
                        explanation = eq.explanation.trim()
                    )
                } else null
            }
            val selectedSubjectName = subjects.firstOrNull { it.id == selectedSubjectId }?.name.orEmpty()

            Button(
                onClick = {
                    val model = (initial ?: Exam()).copy(
                        subjectId = selectedSubjectId,
                        subjectName = selectedSubjectName.ifBlank { "مادة مساحية" },
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        durationMinutes = durationText.toLongOrNull()?.coerceIn(1L, 240L) ?: 10L,
                        passingScorePercent = passingText.toLongOrNull()?.coerceIn(1L, 100L) ?: 60L,
                        questions = builtQuestions,
                        published = true
                    )
                    onSave(model, initial != null)
                },
                enabled = selectedSubjectId.isNotBlank() && title.trim().length >= 2 && builtQuestions.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("حفظ الامتحان")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
