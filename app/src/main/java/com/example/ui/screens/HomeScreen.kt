package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StatMetricTile
import com.example.ui.components.SubjectProgressCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AcademyDashboardState

@Composable
fun HomeScreen(
    state: AcademyDashboardState,
    fallbackName: String,
    onOpenSubject: (String) -> Unit,
    onOpenAllSubjects: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val studentName = state.profile?.fullName?.ifBlank { fallbackName } ?: fallbackName.ifBlank { "طالب المساحة" }
    val firstName = studentName.split(" ").firstOrNull().orEmpty().ifBlank { studentName }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Welcome & Overall Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_surveying),
                            contentDescription = "خلفية أكاديمية المساحة",
                            modifier = Modifier
                                .matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            EmeraldDark.copy(alpha = 0.94f),
                                            EmeraldPrimary.copy(alpha = 0.86f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = GoldAccent.copy(alpha = 0.24f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))
                                    ) {
                                        Text(
                                            text = state.profile?.department?.ifBlank { "الفرقة الأولى • معهد المساحة" }
                                                ?: "الفرقة الأولى • معهد المساحة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "أهلاً بك يا $firstName",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "واصل تقدمك في مواد الفرقة الأولى واستعد للتفوق في الامتحانات.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Progress Ring
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { state.overallProgressFraction },
                                        modifier = Modifier.size(72.dp),
                                        color = GoldAccent,
                                        trackColor = Color.White.copy(alpha = 0.25f),
                                        strokeWidth = 7.dp,
                                        strokeCap = StrokeCap.Round
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${state.overallProgressPercent}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "التقدم",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Quick Large Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenAllSubjects,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_browse_subjects_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = EmeraldDark
                                    )
                                ) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("المواد الدراسية", style = MaterialTheme.typography.labelLarge)
                                }

                                OutlinedButton(
                                    onClick = onOpenExams,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_start_exam_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("الامتحانات", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            // KPI Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        title = "الدروس المكتملة",
                        value = "${state.completedLessonsCount}/${state.totalLessonsCount}",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "متوسط الدرجات",
                        value = "${state.averageExamScore}%",
                        icon = Icons.Filled.TrendingUp,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "امتحانات مجتازة",
                        value = "${state.passedExamsCount}/${state.exams.size}",
                        icon = Icons.Filled.EmojiEvents,
                        accentColor = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Admin Quick Banner (visible strictly to verified Admin)
            if (state.isAdmin) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_admin_panel_card")
                            .clickable(onClick = onOpenAdmin),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))
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
                                    .background(EmeraldDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "لوحة تحكم المدير",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "لوحة تحكم المدير مفعّلة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "إضافة وتعديل وحذف المواد والدروس والفيديوهات والامتحانات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Subjects Overview Header
            item {
                SectionHeaderRow(
                    title = "مواد الفرقة الأولى (${state.subjects.size})",
                    subtitle = "اختر مادة للوصول إلى الدروس والفيديوهات والملفات",
                    actionLabel = "عرض الكل",
                    onActionClick = onOpenAllSubjects
                )
            }

            // Display all subjects on Home so progress per subject is immediately visible
            items(state.subjects, key = { it.id }) { subject ->
                val subjLessons = state.lessonsForSubject(subject.id)
                val doneCount = subjLessons.count { it.id in state.completedLessonIds }
                SubjectProgressCard(
                    subject = subject,
                    lessonsCount = subjLessons.size,
                    completedCount = doneCount,
                    onClick = { onOpenSubject(subject.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
