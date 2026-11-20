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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StatMetricTile
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AcademyDashboardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    state: AcademyDashboardState,
    email: String,
    fallbackName: String,
    isCloudSignedIn: Boolean = true,
    cloudSyncAvailable: Boolean = true,
    onUpdateProfile: (fullName: String, studentCode: String, department: String, phone: String?) -> Unit,
    onOpenAdmin: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onSignOut: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val displayName = state.profile?.fullName?.ifBlank { fallbackName } ?: fallbackName.ifBlank { "طالب المساحة" }
    val studentCode = state.profile?.studentCode?.ifBlank { "غير مسجل" } ?: "غير مسجل"
    val department = state.profile?.department?.ifBlank { "الفرقة الأولى - معهد المساحة" } ?: "الفرقة الأولى - معهد المساحة"
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")) }

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
            // Student Academic Identity Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(EmeraldDark, EmeraldPrimary)))
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "صورة الطالب",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = GoldAccent.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (state.isAdmin) "مدير الأكاديمية" else "طالب مسجل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isCloudSignedIn && email.isNotBlank()) email else "وضع الدخول المباشر (محفوظ على الجهاز)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Badge, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الكود الجامعي: $studentCode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = department,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("profile_edit_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = EmeraldDark
                                    )
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تعديل البيانات", fontWeight = FontWeight.Bold)
                                }

                                if (state.isAdmin) {
                                    OutlinedButton(
                                        onClick = onOpenAdmin,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("profile_admin_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, GoldAccent),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("لوحة المدير")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Progress Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        title = "التقدم العام",
                        value = "${state.overallProgressPercent}%",
                        icon = Icons.Filled.TrendingUp,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "دروس مكتملة",
                        value = "${state.completedLessonsCount}",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "محاولات الاختبار",
                        value = "${state.examResults.size}",
                        icon = Icons.Filled.EmojiEvents,
                        accentColor = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Subject Progress Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تقدمك في المواد الدراسية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        state.subjects.forEach { subj ->
                            val frac = state.subjectProgressFraction(subj.id)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(subj.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${(frac * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = EmeraldPrimary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }

            // Exam Results History
            item {
                SectionHeaderRow(
                    title = "سجل درجات الامتحانات (${state.examResults.size})",
                    subtitle = "محفوظ تلقائياً في قاعدة البيانات السحابية"
                )
            }

            if (state.examResults.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Quiz,
                        title = "لم تقم بأداء أي امتحان بعد",
                        subtitle = "انتقل إلى قسم الامتحانات لاختبار مستواك وحفظ درجاتك هنا."
                    )
                }
            } else {
                items(state.examResults, key = { it.id }) { res ->
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
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (res.passed) SuccessContainer else ErrorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${res.percentage}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.passed) SuccessGreen else ErrorCrimson
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = res.examTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${res.subjectName} • الدرجة: ${res.score}/${res.totalQuestions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val dateStr = res.submittedAt?.toDate()?.let { dateFormatter.format(it) }
                                    ?: dateFormatter.format(Date())
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (res.passed) SuccessContainer else ErrorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (res.passed) "ناجح" else "إعادة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (res.passed) SuccessGreen else ErrorCrimson,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Optional Cloud Sync (Google Sign-In) or Switch Account / Sign Out
            item {
                if (!isCloudSignedIn) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (cloudSyncAvailable) "المزامنة السحابية (اختياري)" else "الحفظ المحلي التلقائي مفعل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (cloudSyncAvailable) {
                                    "جميع دروسك وتقدمك ودرجاتك محفوظة الآن على جهازك تلقائياً دون الحاجة لتسجيل الدخول. يمكنك ربط حسابك في Google لاحقاً لمزامنة بياناتك عبر السحابة."
                                } else {
                                    "جميع دروسك وتقدمك ودرجاتك وتعديلات لوحة المدير محفوظة تلقائياً على جهازك وتعمل بدون إنترنت ودون الحاجة لتسجيل دخول."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (cloudSyncAvailable) {
                                Button(
                                    onClick = onGoogleSignIn,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("profile_google_sign_in_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldDark,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تسجيل الدخول بحساب Google للمزامنة",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("profile_switch_account_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تبديل الحساب / العودة للوضع المباشر",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("profile_sign_out_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, ErrorCrimson.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل الخروج من المزامنة السحابية", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            initialName = displayName,
            initialCode = state.profile?.studentCode.orEmpty(),
            initialDepartment = department,
            initialPhone = state.profile?.phone.orEmpty(),
            onDismiss = { showEditDialog = false },
            onSave = { name, code, dept, phone ->
                onUpdateProfile(name, code, dept, phone)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialCode: String,
    initialDepartment: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var code by remember { mutableStateOf(initialCode) }
    var dept by remember { mutableStateOf(initialDepartment) }
    var phone by remember { mutableStateOf(initialPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الطالب", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رقم الجلوس / الكود") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = { Text("الفرقة والقسم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), code.trim(), dept.trim(), phone.trim().ifBlank { null }) },
                enabled = name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
