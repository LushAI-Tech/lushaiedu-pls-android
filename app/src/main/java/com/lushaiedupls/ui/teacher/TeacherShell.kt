package com.lushaiedupls.ui.teacher

import android.net.Uri
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
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.student.ai.StudentAiChatRoute
import com.lushaiedupls.ui.student.menu.LegalDocumentScreen
import com.lushaiedupls.ui.student.menu.StudentAccountRoute
import com.lushaiedupls.ui.student.secondary.ChaptersScreen
import com.lushaiedupls.ui.student.secondary.ChaptersViewModel
import com.lushaiedupls.ui.student.secondary.NotificationsScreen
import com.lushaiedupls.ui.student.secondary.NotificationsViewModel
import com.lushaiedupls.ui.student.secondary.QuizScreen
import com.lushaiedupls.ui.student.secondary.QuizViewModel
import com.lushaiedupls.ui.teacher.ai.TeacherAiHubRoute
import com.lushaiedupls.ui.teacher.calendar.TeacherCalendarRoute
import com.lushaiedupls.ui.teacher.groups.TeacherMyGroupsRoute
import com.lushaiedupls.ui.teacher.home.TeacherHomeRoute
import com.lushaiedupls.ui.teacher.menu.TeacherMenuOverlay
import com.lushaiedupls.ui.teacher.overview.TeacherClassOverviewRoute
import com.lushaiedupls.ui.teacher.overview.TeacherOverviewRoute
import com.lushaiedupls.ui.teacher.overview.TeacherTakeAttendanceRoute
import com.lushaiedupls.ui.teacher.secondary.TeacherAnnouncementsRoute
import com.lushaiedupls.ui.teacher.secondary.TeacherMoreScreen
import com.lushaiedupls.ui.teacher.secondary.TeacherNewAnnouncementRoute
import com.lushaiedupls.ui.teacher.secondary.TeacherTimetableRoute
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.navigation.lushEnterTransition
import com.lushaiedupls.ui.navigation.lushExitTransition
import com.lushaiedupls.ui.navigation.lushPopEnterTransition
import com.lushaiedupls.ui.navigation.lushPopExitTransition

private val TeacherTabRoutes = TeacherTab.entries.map { it.route }.toSet()

@Composable
fun TeacherShell(
    userSessionStore: UserSessionStore,
    teacherRepository: TeacherRepository,
    studentRepository: StudentRepository,
    authRepository: AuthRepository,
    onLogOut: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val inClassOverview = currentRoute?.startsWith("teacher_class_overview") == true
    val inTakeAttendance = currentRoute?.startsWith("teacher_take_attendance") == true
    val inAiChat = currentRoute?.startsWith("teacher_ai_chats") == true
    val inAiChapters = currentRoute?.startsWith("teacher_ai_chapters") == true
    val inAiFlow = inAiChat || inAiChapters
    val inMoreStack = currentRoute in setOf(
        TeacherRoutes.MORE,
        TeacherRoutes.CHAPTERS,
        TeacherRoutes.QUIZ,
        TeacherRoutes.TIMETABLE,
        TeacherRoutes.ACADEMIC_CALENDAR,
        TeacherRoutes.ANNOUNCEMENTS,
        TeacherRoutes.NEW_ANNOUNCEMENT,
        TeacherRoutes.OVERVIEW,
    ) || inTakeAttendance
    val inProfileStack = currentRoute in setOf(
        TeacherRoutes.ACCOUNT,
        TeacherRoutes.PRIVACY,
        TeacherRoutes.TERMS,
    )
    val selectedTab = when {
        currentRoute == TeacherRoutes.HOME ||
            currentRoute == TeacherRoutes.NOTIFICATIONS ||
            inProfileStack -> TeacherTab.Home
        currentRoute == TeacherRoutes.MY_GROUPS || inClassOverview -> TeacherTab.MyGroups
        currentRoute == TeacherRoutes.AI || inAiFlow -> TeacherTab.Ai
        currentRoute == TeacherRoutes.CALENDAR -> TeacherTab.Calendar
        inMoreStack -> TeacherTab.Menu
        else -> TeacherTab.Home
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
                TeacherBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> navigateTab(tab.route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = TeacherRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { lushEnterTransition(tabRoutes = TeacherTabRoutes) },
            exitTransition = { lushExitTransition(tabRoutes = TeacherTabRoutes) },
            popEnterTransition = { lushPopEnterTransition(tabRoutes = TeacherTabRoutes) },
            popExitTransition = { lushPopExitTransition(tabRoutes = TeacherTabRoutes) },
        ) {
            composable(TeacherRoutes.HOME) {
                TeacherHomeRoute(
                    userSessionStore = userSessionStore,
                    teacherRepository = teacherRepository,
                    onNotificationsClick = {
                        tabNavController.navigate(TeacherRoutes.NOTIFICATIONS)
                    },
                    onProfileClick = { showMenuOverlay = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.NOTIFICATIONS) {
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
                        TeacherErrorBox(state.errorMessage.orEmpty())
                    else -> NotificationsScreen(
                        notifications = state.notifications,
                        onBack = { tabNavController.popBackStack() },
                        onMarkAllRead = vm::markAllRead,
                        onOpenNotification = { vm.markRead(it.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(TeacherRoutes.MY_GROUPS) {
                TeacherMyGroupsRoute(
                    teacherRepository = teacherRepository,
                    onGroupClick = { group ->
                        tabNavController.navigate(TeacherRoutes.classOverview(group.id))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(
                route = TeacherRoutes.CLASS_OVERVIEW,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            ) { entry ->
                val groupId = entry.arguments?.getString("groupId").orEmpty()
                TeacherClassOverviewRoute(
                    groupId = groupId,
                    teacherRepository = teacherRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.OVERVIEW) {
                TeacherOverviewRoute(
                    teacherRepository = teacherRepository,
                    onBack = { tabNavController.popBackStack() },
                    onTakeAttendance = { unitId, dateLabel, periodId, isExtraClass, extraLabel ->
                        tabNavController.navigate(
                            TeacherRoutes.takeAttendance(
                                unitId = unitId,
                                date = dateLabel,
                                periodId = periodId,
                                isExtraClass = isExtraClass,
                                extraLabel = extraLabel,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(
                route = TeacherRoutes.TAKE_ATTENDANCE,
                arguments = listOf(
                    navArgument("unitId") { type = NavType.StringType },
                    navArgument("date") { type = NavType.StringType },
                    navArgument("periodId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("extra") {
                        type = NavType.StringType
                        defaultValue = "0"
                    },
                    navArgument("extraLabel") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val unitId = Uri.decode(entry.arguments?.getString("unitId").orEmpty())
                val date = Uri.decode(entry.arguments?.getString("date").orEmpty())
                val periodId = Uri.decode(entry.arguments?.getString("periodId").orEmpty())
                    .takeIf { it.isNotBlank() }
                val isExtraClass = entry.arguments?.getString("extra") == "1"
                val extraLabel = Uri.decode(entry.arguments?.getString("extraLabel").orEmpty())
                    .takeIf { it.isNotBlank() }
                TeacherTakeAttendanceRoute(
                    unitId = unitId,
                    dateLabel = date,
                    periodId = periodId,
                    isExtraClass = isExtraClass,
                    extraLabel = extraLabel,
                    teacherRepository = teacherRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.AI) {
                TeacherAiHubRoute(
                    studentRepository = studentRepository,
                    teacherRepository = teacherRepository,
                    onSubjectClick = { subject ->
                        tabNavController.navigate(
                            TeacherRoutes.aiChapters(subject.id, subject.name),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(
                route = TeacherRoutes.AI_CHAPTERS,
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
                    key = "teacher-ai-chapters-$subjectId",
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
                        TeacherErrorBox(state.errorMessage.orEmpty())
                    else -> ChaptersScreen(
                        title = state.subjectTitle.ifBlank { subjectName },
                        stats = state.stats ?: SubjectChapterStats("0%", "0%", "0", "0"),
                        chapters = state.chapters,
                        onBack = { tabNavController.popBackStack() },
                        onChapterClick = { chapter ->
                            tabNavController.navigate(
                                TeacherRoutes.aiChats(subjectId, chapter.id),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(
                route = TeacherRoutes.AI_CHATS,
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
                            TeacherRoutes.quiz(chapterId = quizChapterId, sectionIds = sectionIds),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.CALENDAR) {
                TeacherTimetableRoute(
                    teacherRepository = teacherRepository,
                    editable = false,
                    onBack = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.ACADEMIC_CALENDAR) {
                TeacherCalendarRoute(
                    teacherRepository = teacherRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.MORE) {
                TeacherMoreScreen(
                    onAcademicCalendar = {
                        tabNavController.navigate(TeacherRoutes.ACADEMIC_CALENDAR)
                    },
                    onAnnouncements = {
                        tabNavController.navigate(TeacherRoutes.ANNOUNCEMENTS)
                    },
                    onAttendance = { tabNavController.navigate(TeacherRoutes.OVERVIEW) },
                    onTimetable = { tabNavController.navigate(TeacherRoutes.TIMETABLE) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.ACCOUNT) {
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
            composable(TeacherRoutes.PRIVACY) {
                LegalDocumentScreen(
                    title = stringResource(R.string.privacy_title),
                    body = stringResource(R.string.privacy_body),
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.TERMS) {
                LegalDocumentScreen(
                    title = stringResource(R.string.terms_title),
                    body = stringResource(R.string.terms_body),
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.CHAPTERS) {
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
                        TeacherErrorBox(state.errorMessage.orEmpty())
                    else -> ChaptersScreen(
                        title = state.subjectTitle,
                        stats = state.stats ?: SubjectChapterStats("0%", "0%", "0", "0"),
                        chapters = state.chapters,
                        onBack = { tabNavController.popBackStack() },
                        onChapterClick = { chapter ->
                            tabNavController.navigate(TeacherRoutes.quiz(chapter.id))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(
                route = TeacherRoutes.QUIZ,
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
                        TeacherErrorBox(state.errorMessage.orEmpty())
                    state.questions.isEmpty() -> TeacherErrorBox("No quiz questions.")
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
            composable(TeacherRoutes.TIMETABLE) {
                TeacherTimetableRoute(
                    teacherRepository = teacherRepository,
                    editable = true,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.ANNOUNCEMENTS) {
                TeacherAnnouncementsRoute(
                    teacherRepository = teacherRepository,
                    onBack = { tabNavController.popBackStack() },
                    onCreate = {
                        tabNavController.navigate(TeacherRoutes.NEW_ANNOUNCEMENT)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(TeacherRoutes.NEW_ANNOUNCEMENT) {
                TeacherNewAnnouncementRoute(
                    teacherRepository = teacherRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showMenuOverlay) {
        TeacherMenuOverlay(
            onDismiss = { showMenuOverlay = false },
            onSwitchRole = { role ->
                showMenuOverlay = false
                onSwitchRole(role)
            },
            onAccount = {
                showMenuOverlay = false
                tabNavController.navigate(TeacherRoutes.ACCOUNT)
            },
            onPrivacy = {
                showMenuOverlay = false
                tabNavController.navigate(TeacherRoutes.PRIVACY)
            },
            onTerms = {
                showMenuOverlay = false
                tabNavController.navigate(TeacherRoutes.TERMS)
            },
            onLogOut = {
                showMenuOverlay = false
                onLogOut()
            },
        )
    }
}

/** Kept for call-site compatibility; prefer [TeacherShell]. */
@Composable
fun TeacherShellScaffold(
    userSessionStore: UserSessionStore,
    teacherRepository: TeacherRepository,
    studentRepository: StudentRepository,
    authRepository: AuthRepository,
    onLogOut: () -> Unit,
    onSwitchRole: (UserRole) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TeacherShell(
        userSessionStore = userSessionStore,
        teacherRepository = teacherRepository,
        studentRepository = studentRepository,
        authRepository = authRepository,
        onLogOut = onLogOut,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
    )
}

@Composable
private fun TeacherErrorBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = BrandOrange)
    }
}
