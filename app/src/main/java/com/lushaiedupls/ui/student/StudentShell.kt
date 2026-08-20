package com.lushaiedupls.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.SubjectChapterStats
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.student.ai.StudentAiChatRoute
import com.lushaiedupls.ui.student.ai.StudentAiHubRoute
import com.lushaiedupls.ui.student.attendance.StudentAttendanceRoute
import com.lushaiedupls.ui.student.calendar.StudentCalendarRoute
import com.lushaiedupls.ui.student.home.StudentHomeRoute
import com.lushaiedupls.ui.student.linkparent.StudentLinkParentRoute
import com.lushaiedupls.ui.student.menu.LegalDocumentScreen
import com.lushaiedupls.ui.student.menu.StudentAccountRoute
import com.lushaiedupls.ui.student.menu.StudentMenuOverlay
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.student.secondary.ChaptersScreen
import com.lushaiedupls.ui.student.secondary.ChaptersViewModel
import com.lushaiedupls.ui.student.secondary.MoreRoute
import com.lushaiedupls.ui.student.secondary.NotificationsScreen
import com.lushaiedupls.ui.student.secondary.NotificationsViewModel
import com.lushaiedupls.ui.student.secondary.QuizScreen
import com.lushaiedupls.ui.student.secondary.QuizViewModel
import com.lushaiedupls.ui.student.secondary.TimetableRoute
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.navigation.lushEnterTransition
import com.lushaiedupls.ui.navigation.lushExitTransition
import com.lushaiedupls.ui.navigation.lushPopEnterTransition
import com.lushaiedupls.ui.navigation.lushPopExitTransition

private val StudentTabRoutes = StudentTab.entries.map { it.route }.toSet()

@Composable
fun StudentShell(
    userSessionStore: UserSessionStore,
    studentRepository: StudentRepository,
    authRepository: AuthRepository,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val inAiChat = currentRoute?.startsWith("student_ai_chats") == true
    val inAiChapters = currentRoute?.startsWith("student_ai_chapters") == true
    val inAiFlow = inAiChat || inAiChapters
    val inMoreStack = currentRoute in setOf(
        StudentRoutes.MORE,
        StudentRoutes.CHAPTERS,
        StudentRoutes.CALENDAR,
    ) || currentRoute?.startsWith("student_quiz") == true
    val inProfileStack = currentRoute in setOf(
        StudentRoutes.ACCOUNT,
        StudentRoutes.LINK_PARENT,
        StudentRoutes.PRIVACY,
        StudentRoutes.TERMS,
    )
    val selectedTab = when {
        currentRoute == StudentRoutes.HOME ||
            currentRoute == StudentRoutes.NOTIFICATIONS ||
            inProfileStack -> StudentTab.Home
        currentRoute == StudentRoutes.TIMETABLE -> StudentTab.Timetable
        currentRoute == StudentRoutes.AI || inAiFlow -> StudentTab.Ai
        currentRoute == StudentRoutes.ATTENDANCE -> StudentTab.Attendance
        inMoreStack -> StudentTab.More
        else -> StudentTab.Home
    }
    var showMenuOverlay by remember { mutableStateOf(false) }

    fun navigateTab(route: String) {
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding(),
        containerColor = BgWhite,
        bottomBar = {
            if (!inAiFlow) {
                StudentBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> navigateTab(tab.route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = StudentRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { lushEnterTransition(tabRoutes = StudentTabRoutes) },
            exitTransition = { lushExitTransition(tabRoutes = StudentTabRoutes) },
            popEnterTransition = { lushPopEnterTransition(tabRoutes = StudentTabRoutes) },
            popExitTransition = { lushPopExitTransition(tabRoutes = StudentTabRoutes) },
        ) {
            composable(StudentRoutes.HOME) {
                StudentHomeRoute(
                    userSessionStore = userSessionStore,
                    studentRepository = studentRepository,
                    onNotificationsClick = {
                        tabNavController.navigate(StudentRoutes.NOTIFICATIONS)
                    },
                    onProfileClick = { showMenuOverlay = true },
                )
            }
            composable(StudentRoutes.NOTIFICATIONS) {
                val vm: NotificationsViewModel = viewModel(
                    factory = NotificationsViewModel.provideFactory(studentRepository),
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                when {
                    state.isLoading && state.notifications.isEmpty() -> StudentPageSkeleton(
                        kind = StudentSkeletonKind.Notifications,
                        title = stringResource(R.string.notifications_title),
                    )
                    state.errorMessage != null && state.notifications.isEmpty() ->
                        ErrorBox(state.errorMessage.orEmpty())
                    else -> NotificationsScreen(
                        notifications = state.notifications,
                        onBack = { tabNavController.popBackStack() },
                        onMarkAllRead = vm::markAllRead,
                        onOpenNotification = { vm.markRead(it.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(StudentRoutes.CALENDAR) {
                StudentCalendarRoute(
                    studentRepository = studentRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.AI) {
                StudentAiHubRoute(
                    studentRepository = studentRepository,
                    onSubjectClick = { subject ->
                        tabNavController.navigate(
                            StudentRoutes.aiChapters(subject.id, subject.name),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(
                route = StudentRoutes.AI_CHAPTERS,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType },
                    navArgument("name") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { entry ->
                val subjectId = entry.arguments?.getString("subjectId").orEmpty()
                val subjectName = entry.arguments?.getString("name").orEmpty()
                val vm: ChaptersViewModel = viewModel(
                    key = "ai-chapters-$subjectId",
                    factory = ChaptersViewModel.provideFactory(
                        studentRepository,
                        subjectId,
                        subjectName,
                    ),
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                when {
                    state.isLoading && state.chapters.isEmpty() -> StudentPageSkeleton(
                        kind = StudentSkeletonKind.List,
                        title = subjectName.ifBlank { stringResource(R.string.chapters_subject_title) },
                    )
                    state.errorMessage != null && state.chapters.isEmpty() ->
                        ErrorBox(state.errorMessage.orEmpty())
                    else -> ChaptersScreen(
                        title = state.subjectTitle.ifBlank { subjectName },
                        stats = state.stats ?: SubjectChapterStats("0%", "0%", "0", "0"),
                        chapters = state.chapters,
                        onBack = { tabNavController.popBackStack() },
                        onChapterClick = { chapter ->
                            tabNavController.navigate(
                                StudentRoutes.aiChats(subjectId, chapter.id),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(
                route = StudentRoutes.AI_CHATS,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType },
                    navArgument("chapterId") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { entry ->
                val subjectId = entry.arguments?.getString("subjectId").orEmpty()
                val chapterId = entry.arguments?.getString("chapterId").orEmpty()
                StudentAiChatRoute(
                    subjectId = subjectId,
                    chapterId = chapterId.takeIf { it.isNotBlank() },
                    studentRepository = studentRepository,
                    onBack = { tabNavController.popBackStack() },
                    onTakeQuiz = { quizChapterId, sectionIds ->
                        tabNavController.navigate(
                            StudentRoutes.quiz(chapterId = quizChapterId, sectionIds = sectionIds),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.ATTENDANCE) {
                StudentAttendanceRoute(
                    studentRepository = studentRepository,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.MORE) {
                MoreRoute(
                    studentRepository = studentRepository,
                    onAcademicCalendar = { tabNavController.navigate(StudentRoutes.CALENDAR) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.CHAPTERS) {
                val vm: ChaptersViewModel = viewModel(
                    factory = ChaptersViewModel.provideFactory(studentRepository),
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                when {
                    state.isLoading && state.chapters.isEmpty() -> StudentPageSkeleton(
                        kind = StudentSkeletonKind.List,
                        title = stringResource(R.string.chapters_subject_title),
                    )
                    state.errorMessage != null && state.chapters.isEmpty() ->
                        ErrorBox(state.errorMessage.orEmpty())
                    else -> ChaptersScreen(
                        title = state.subjectTitle,
                        stats = state.stats ?: SubjectChapterStats("0%", "0%", "0", "0"),
                        chapters = state.chapters,
                        onBack = { tabNavController.popBackStack() },
                        onChapterClick = { chapter ->
                            tabNavController.navigate(StudentRoutes.quiz(chapter.id))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(
                route = StudentRoutes.QUIZ,
                arguments = listOf(
                    navArgument("chapterId") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                    navArgument("sectionId") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { entry ->
                val chapterId = entry.arguments?.getString("chapterId").orEmpty()
                val sectionId = entry.arguments?.getString("sectionId").orEmpty()
                val vm: QuizViewModel = viewModel(
                    factory = QuizViewModel.provideFactory(
                        studentRepository,
                        chapterId.takeIf { it.isNotBlank() },
                        sectionId.takeIf { it.isNotBlank() },
                    ),
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                when {
                    state.isLoading && state.questions.isEmpty() -> StudentPageSkeleton(
                        kind = StudentSkeletonKind.List,
                        title = stringResource(R.string.quiz_title),
                    )
                    state.errorMessage != null && state.questions.isEmpty() ->
                        ErrorBox(state.errorMessage.orEmpty())
                    state.questions.isEmpty() -> ErrorBox("No quiz questions.")
                    else -> QuizScreen(
                        questions = state.questions,
                        selectedByQuestionId = state.selectedByQuestionId,
                        isSubmitting = state.isSubmitting,
                        result = state.result,
                        errorMessage = state.errorMessage,
                        onSelectAnswer = vm::selectAnswer,
                        onSubmit = vm::submit,
                        onBack = { tabNavController.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(StudentRoutes.TIMETABLE) {
                TimetableRoute(
                    studentRepository = studentRepository,
                    onBack = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.ACCOUNT) {
                StudentAccountRoute(
                    userSessionStore = userSessionStore,
                    studentRepository = studentRepository,
                    authRepository = authRepository,
                    onBack = { tabNavController.popBackStack() },
                    onLogOut = onLogOut,
                    onDeleteAccountConfirmed = onLogOut,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.LINK_PARENT) {
                StudentLinkParentRoute(
                    studentRepository = studentRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.PRIVACY) {
                LegalDocumentScreen(
                    title = stringResource(R.string.privacy_title),
                    body = stringResource(R.string.privacy_body),
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(StudentRoutes.TERMS) {
                LegalDocumentScreen(
                    title = stringResource(R.string.terms_title),
                    body = stringResource(R.string.terms_body),
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showMenuOverlay) {
        StudentMenuOverlay(
            onDismiss = { showMenuOverlay = false },
            onAccount = {
                showMenuOverlay = false
                tabNavController.navigate(StudentRoutes.ACCOUNT)
            },
            onLinkParent = {
                showMenuOverlay = false
                tabNavController.navigate(StudentRoutes.LINK_PARENT)
            },
            onPrivacy = {
                showMenuOverlay = false
                tabNavController.navigate(StudentRoutes.PRIVACY)
            },
            onTerms = {
                showMenuOverlay = false
                tabNavController.navigate(StudentRoutes.TERMS)
            },
            onLogOut = {
                showMenuOverlay = false
                onLogOut()
            },
        )
    }
}

@Composable
private fun ErrorBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = BrandOrange)
    }
}
