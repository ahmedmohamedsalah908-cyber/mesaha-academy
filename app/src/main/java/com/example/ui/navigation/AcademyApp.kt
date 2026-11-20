package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.firebase.AcademyRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.signOutUser
import com.example.data.local.LocalAcademyStore
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CompleteStudentRegistrationScreen
import com.example.ui.screens.ExamTakingScreen
import com.example.ui.screens.ExamsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSoftContainer
import com.example.viewmodel.AcademyDashboardState
import com.example.viewmodel.AcademyViewModel
import com.example.viewmodel.LocalAcademyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object AcademyRoutes {
    const val HOME = "home"
    const val SUBJECTS = "subjects"
    const val EXAMS = "exams"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val SUBJECT_DETAIL = "subject_detail/{subjectId}"
    const val LESSON_DETAIL = "lesson_detail/{lessonId}"
    const val EXAM_TAKE = "exam_take/{examId}"

    fun subjectDetail(subjectId: String) = "subject_detail/$subjectId"
    fun lessonDetail(lessonId: String) = "lesson_detail/$lessonId"
    fun examTake(examId: String) = "exam_take/$examId"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Opens directly into Mesaha Academy without forcing a login wall.
 * - Unauthenticated: uses strictly on-device LocalAcademyShell (ZERO Firestore listeners/queries).
 * - Authenticated via optional Google Sign-In in Profile: switches seamlessly to cloud-synced
 *   AuthenticatedAcademyShell keyed by uid.
 */
@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        LocalAcademyShell()
    } else {
        key(user.uid) {
            AuthenticatedAcademyShell(
                currentUserId = user.uid,
                currentUserEmail = user.email.orEmpty(),
                isEmailVerified = user.isEmailVerified,
                displayName = user.displayName.orEmpty(),
                photoUrl = user.photoUrl?.toString()
            )
        }
    }
}

@Composable
private fun LocalAcademyShell() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: LocalAcademyViewModel = viewModel(
        key = "local_open_academy",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                LocalAcademyViewModel(LocalAcademyStore(app))
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    AcademyMainScaffold(
        state = state,
        snackbarHostState = snackbarHostState,
        email = "",
        fallbackName = state.profile?.fullName ?: "طالب الفرقة الأولى",
        isCloudSignedIn = false,
        onToggleLessonCompleted = { lesson, done -> viewModel.toggleLessonCompleted(lesson, done) },
        onSubmitExam = { exam, score, total, onSaved -> viewModel.submitExamAttempt(exam, score, total, onSaved) },
        onUpdateProfile = { name, code, dept, phone -> viewModel.saveStudentProfile(name, code, dept, phone) },
        onSeedCurriculum = { viewModel.resetCurriculum() },
        onSaveSubject = { subject, isEdit -> viewModel.saveSubject(subject, isEdit) },
        onDeleteSubject = { id -> viewModel.deleteSubject(id) },
        onSaveLesson = { lesson, isEdit -> viewModel.saveLesson(lesson, isEdit) },
        onDeleteLesson = { id -> viewModel.deleteLesson(id) },
        onSaveExam = { exam, isEdit -> viewModel.saveExam(exam, isEdit) },
        onDeleteExam = { id -> viewModel.deleteExam(id) },
        onNotify = { msg -> viewModel.notifyUser(msg) },
        onGoogleSignIn = {
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { viewModel.notifyUser("تم ربط حسابك في Google وتفعيل المزامنة السحابية") },
                onAuthError = { msg -> viewModel.notifyUser(msg) },
                scope = scope,
                onAuthCancelled = { viewModel.notifyUser("تم إلغاء تسجيل الدخول بحساب Google") },
                clearPreviousSelection = true
            )
        },
        onSignOut = {}
    )
}

@Composable
private fun AuthenticatedAcademyShell(
    currentUserId: String,
    currentUserEmail: String,
    isEmailVerified: Boolean,
    displayName: String,
    photoUrl: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AcademyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AcademyViewModel(
                    repository = AcademyRepository(db),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    isEmailVerified = isEmailVerified,
                    googleDisplayName = displayName,
                    googlePhotoUrl = photoUrl
                )
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (state.profileLoaded && state.profile == null) {
        CompleteStudentRegistrationScreen(
            defaultName = displayName.ifBlank { currentUserEmail.substringBefore("@") },
            email = currentUserEmail,
            isBusy = state.isBusy,
            onSubmit = { fullName, studentCode, department, phone ->
                viewModel.saveStudentProfile(
                    fullName = fullName,
                    studentCode = studentCode,
                    department = department,
                    phone = phone,
                    isExisting = false
                )
            }
        )
        return
    }

    AcademyMainScaffold(
        state = state,
        snackbarHostState = snackbarHostState,
        email = currentUserEmail,
        fallbackName = displayName,
        isCloudSignedIn = true,
        onToggleLessonCompleted = { lesson, done -> viewModel.toggleLessonCompleted(lesson, done) },
        onSubmitExam = { exam, score, total, onSaved -> viewModel.submitExamAttempt(exam, score, total, onSaved) },
        onUpdateProfile = { name, code, dept, phone ->
            viewModel.saveStudentProfile(
                fullName = name,
                studentCode = code,
                department = dept,
                phone = phone,
                isExisting = state.profile != null
            )
        },
        onSeedCurriculum = { viewModel.seedCurriculum(forceOverwrite = true) },
        onSaveSubject = { subject, isEdit -> viewModel.saveSubject(subject, isEdit) },
        onDeleteSubject = { id -> viewModel.deleteSubject(id) },
        onSaveLesson = { lesson, isEdit -> viewModel.saveLesson(lesson, isEdit) },
        onDeleteLesson = { id -> viewModel.deleteLesson(id) },
        onSaveExam = { exam, isEdit -> viewModel.saveExam(exam, isEdit) },
        onDeleteExam = { id -> viewModel.deleteExam(id) },
        onNotify = { msg -> viewModel.notifyUser(msg) },
        onGoogleSignIn = {},
        onSignOut = {
            signOutUser(credentialManager = credentialManager, scope = scope)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademyMainScaffold(
    state: AcademyDashboardState,
    snackbarHostState: SnackbarHostState,
    email: String,
    fallbackName: String,
    isCloudSignedIn: Boolean,
    onToggleLessonCompleted: (Lesson, Boolean) -> Unit,
    onSubmitExam: (Exam, Int, Int, (ExamResult) -> Unit) -> Unit,
    onUpdateProfile: (String, String, String, String?) -> Unit,
    onSeedCurriculum: () -> Unit,
    onSaveSubject: (Subject, Boolean) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onSaveLesson: (Lesson, Boolean) -> Unit,
    onDeleteLesson: (String) -> Unit,
    onSaveExam: (Exam, Boolean) -> Unit,
    onDeleteExam: (String) -> Unit,
    onNotify: (String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = remember(state.isAdmin) {
        buildList {
            add(NavTabItem(AcademyRoutes.HOME, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"))
            add(NavTabItem(AcademyRoutes.SUBJECTS, "المواد", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_subjects"))
            add(NavTabItem(AcademyRoutes.EXAMS, "الامتحانات", Icons.Filled.Quiz, Icons.Outlined.Quiz, "nav_exams"))
            add(NavTabItem(AcademyRoutes.PROFILE, "ملفي", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"))
            if (state.isAdmin) {
                add(NavTabItem(AcademyRoutes.ADMIN, "المدير", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_admin"))
            }
        }
    }

    val isTopLevelRoute = tabs.any { it.route == currentRoute }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isTopLevelRoute) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "أكاديمية المساحة",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = EmeraldDark,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldDark,
                                    selectedTextColor = EmeraldDark,
                                    indicatorColor = EmeraldSoftContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EmeraldDark,
                                    selectedTextColor = EmeraldDark,
                                    indicatorColor = EmeraldSoftContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = AcademyRoutes.HOME
                    ) {
                        composable(AcademyRoutes.HOME) {
                            HomeScreen(
                                state = state,
                                fallbackName = fallbackName,
                                onOpenSubject = { id -> navController.navigate(AcademyRoutes.subjectDetail(id)) },
                                onOpenAllSubjects = { navigateTopLevel(AcademyRoutes.SUBJECTS) },
                                onOpenExams = { navigateTopLevel(AcademyRoutes.EXAMS) },
                                onOpenAdmin = { navigateTopLevel(AcademyRoutes.ADMIN) }
                            )
                        }

                        composable(AcademyRoutes.SUBJECTS) {
                            SubjectsScreen(
                                state = state,
                                onOpenSubject = { id -> navController.navigate(AcademyRoutes.subjectDetail(id)) }
                            )
                        }

                        composable(
                            route = AcademyRoutes.SUBJECT_DETAIL,
                            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
                        ) { entry ->
                            val subjectId = entry.arguments?.getString("subjectId").orEmpty()
                            val subject = state.subjects.firstOrNull { it.id == subjectId }
                            SubjectDetailScreen(
                                subject = subject,
                                lessons = state.lessonsForSubject(subjectId),
                                exams = state.examsForSubject(subjectId),
                                completedLessonIds = state.completedLessonIds,
                                onBack = { navController.popBackStack() },
                                onOpenLesson = { lessonId -> navController.navigate(AcademyRoutes.lessonDetail(lessonId)) },
                                onStartExam = { examId -> navController.navigate(AcademyRoutes.examTake(examId)) }
                            )
                        }

                        composable(
                            route = AcademyRoutes.LESSON_DETAIL,
                            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
                        ) { entry ->
                            val lessonId = entry.arguments?.getString("lessonId").orEmpty()
                            val lesson = state.lessons.firstOrNull { it.id == lessonId }
                            val subjectName = state.subjects.firstOrNull { it.id == lesson?.subjectId }?.name ?: "مادة مساحية"
                            val completed = lessonId in state.completedLessonIds
                            LessonDetailScreen(
                                lesson = lesson,
                                subjectName = subjectName,
                                isCompleted = completed,
                                onBack = { navController.popBackStack() },
                                onToggleCompleted = {
                                    if (lesson != null) {
                                        onToggleLessonCompleted(lesson, completed)
                                    }
                                },
                                onNotify = onNotify
                            )
                        }

                        composable(AcademyRoutes.EXAMS) {
                            ExamsScreen(
                                state = state,
                                onStartExam = { examId -> navController.navigate(AcademyRoutes.examTake(examId)) }
                            )
                        }

                        composable(
                            route = AcademyRoutes.EXAM_TAKE,
                            arguments = listOf(navArgument("examId") { type = NavType.StringType })
                        ) { entry ->
                            val examId = entry.arguments?.getString("examId").orEmpty()
                            val exam = state.exams.firstOrNull { it.id == examId }
                            ExamTakingScreen(
                                exam = exam,
                                onBack = { navController.popBackStack() },
                                onSubmitResult = { score, total, onSaved ->
                                    if (exam != null) {
                                        onSubmitExam(exam, score, total, onSaved)
                                    }
                                }
                            )
                        }

                        composable(AcademyRoutes.PROFILE) {
                            ProfileScreen(
                                state = state,
                                email = email,
                                fallbackName = fallbackName,
                                isCloudSignedIn = isCloudSignedIn,
                                onUpdateProfile = onUpdateProfile,
                                onOpenAdmin = { navigateTopLevel(AcademyRoutes.ADMIN) },
                                onGoogleSignIn = onGoogleSignIn,
                                onSignOut = onSignOut
                            )
                        }

                        composable(AcademyRoutes.ADMIN) {
                            AdminDashboardScreen(
                                state = state,
                                onSeedCurriculum = onSeedCurriculum,
                                onSaveSubject = onSaveSubject,
                                onDeleteSubject = onDeleteSubject,
                                onSaveLesson = onSaveLesson,
                                onDeleteLesson = onDeleteLesson,
                                onSaveExam = onSaveExam,
                                onDeleteExam = onDeleteExam
                            )
                        }
                    }
                }
            }
        }
    }
}
