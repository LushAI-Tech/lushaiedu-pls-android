package com.lushaiedupls.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.navigation.lushEnterTransition
import com.lushaiedupls.ui.navigation.lushExitTransition
import com.lushaiedupls.ui.navigation.lushPopEnterTransition
import com.lushaiedupls.ui.navigation.lushPopExitTransition
import com.lushaiedupls.ui.parent.attendance.ParentChildAttendanceRoute
import com.lushaiedupls.ui.parent.fees.ParentFeesRoute
import com.lushaiedupls.ui.parent.feedback.ParentFeedbackRoute
import com.lushaiedupls.ui.parent.home.ParentHomeRoute
import com.lushaiedupls.ui.parent.menu.ParentMenuOverlay
import com.lushaiedupls.ui.parent.more.ParentMoreScreen
import com.lushaiedupls.ui.parent.notifications.ParentNotificationsViewModel
import com.lushaiedupls.ui.parent.scan.rememberParentQrScanLauncher
import com.lushaiedupls.ui.parent.timetable.ParentChildTimetableRoute
import com.lushaiedupls.ui.student.calendar.StudentCalendarRoute
import com.lushaiedupls.ui.student.menu.LegalDocumentScreen
import com.lushaiedupls.ui.student.menu.StudentAccountRoute
import com.lushaiedupls.ui.student.secondary.NotificationsScreen
import com.lushaiedupls.ui.theme.BgWhite

private val ParentTabRoutes = setOf(
    ParentRoutes.HOME,
    ParentRoutes.CALENDAR,
    ParentRoutes.ATTENDANCE,
    ParentRoutes.MORE,
)

@Composable
fun ParentShell(
    userSessionStore: UserSessionStore,
    parentRepository: ParentRepository,
    studentRepository: StudentRepository,
    authRepository: AuthRepository,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showMenuOverlay by remember { mutableStateOf(false) }
    var selectedStudentId by rememberSaveable { mutableStateOf<String?>(null) }

    val inMoreStack = currentRoute in setOf(
        ParentRoutes.MORE,
        ParentRoutes.TIMETABLE,
        ParentRoutes.FEES,
        ParentRoutes.FEEDBACK,
    )
    val selectedTab = when {
        currentRoute == ParentRoutes.CALENDAR -> ParentTab.Calendar
        currentRoute == ParentRoutes.ATTENDANCE -> ParentTab.Attendance
        inMoreStack -> ParentTab.More
        else -> ParentTab.Home
    }

    fun navigateTab(route: String) {
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val launchQrScanner = rememberParentQrScanLauncher(
        parentRepository = parentRepository,
        onLinked = { navigateTab(ParentRoutes.HOME) },
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding(),
        containerColor = BgWhite,
        bottomBar = {
            ParentBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> navigateTab(tab.route) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = ParentRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { lushEnterTransition(tabRoutes = ParentTabRoutes) },
            exitTransition = { lushExitTransition(tabRoutes = ParentTabRoutes) },
            popEnterTransition = { lushPopEnterTransition(tabRoutes = ParentTabRoutes) },
            popExitTransition = { lushPopExitTransition(tabRoutes = ParentTabRoutes) },
        ) {
            composable(ParentRoutes.HOME) {
                ParentHomeRoute(
                    userSessionStore = userSessionStore,
                    parentRepository = parentRepository,
                    studentRepository = studentRepository,
                    onNotificationsClick = {
                        tabNavController.navigate(ParentRoutes.NOTIFICATIONS)
                    },
                    onProfileClick = { showMenuOverlay = true },
                    onScanClick = launchQrScanner,
                    onChildClick = { studentId, _ ->
                        selectedStudentId = studentId
                        navigateTab(ParentRoutes.ATTENDANCE)
                    },
                    onFeesClick = { tabNavController.navigate(ParentRoutes.FEES) },
                    onStudentReady = { studentId ->
                        selectedStudentId = studentId
                    },
                )
            }
            composable(ParentRoutes.CALENDAR) {
                StudentCalendarRoute(
                    studentRepository = studentRepository,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(ParentRoutes.ATTENDANCE) {
                ParentChildAttendanceRoute(
                    parentRepository = parentRepository,
                    studentId = selectedStudentId,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(ParentRoutes.MORE) {
                ParentMoreScreen(
                    onTimetable = { tabNavController.navigate(ParentRoutes.TIMETABLE) },
                    onFees = { tabNavController.navigate(ParentRoutes.FEES) },
                    onFeedback = { tabNavController.navigate(ParentRoutes.FEEDBACK) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(ParentRoutes.TIMETABLE) {
                ParentChildTimetableRoute(
                    parentRepository = parentRepository,
                    studentId = selectedStudentId,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(ParentRoutes.FEES) {
                ParentFeesRoute(
                    parentRepository = parentRepository,
                    studentId = selectedStudentId,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(ParentRoutes.FEEDBACK) {
                ParentFeedbackRoute(
                    parentRepository = parentRepository,
                    studentId = selectedStudentId,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(ParentRoutes.NOTIFICATIONS) {
                // Use the parent-specific VM that syncs badge via parentRepository
                val vm: ParentNotificationsViewModel = viewModel(
                    factory = ParentNotificationsViewModel.provideFactory(
                        studentRepository = studentRepository,
                        parentRepository = parentRepository,
                    ),
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                when {
                    state.isLoading && state.notifications.isEmpty() -> StudentPageSkeleton(
                        kind = StudentSkeletonKind.Notifications,
                        title = stringResource(R.string.parent_notifications_title),
                    )
                    else -> NotificationsScreen(
                        notifications = state.notifications,
                        onBack = { tabNavController.popBackStack() },
                        onMarkAllRead = vm::markAllRead,
                        onOpenNotification = { vm.markRead(it.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            composable(ParentRoutes.ACCOUNT) {
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
            composable(ParentRoutes.PRIVACY) {
                LegalDocumentScreen(
                    title = stringResource(R.string.privacy_title),
                    body = stringResource(R.string.privacy_body),
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(ParentRoutes.TERMS) {
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
        ParentMenuOverlay(
            onDismiss = { showMenuOverlay = false },
            onAccount = {
                showMenuOverlay = false
                tabNavController.navigate(ParentRoutes.ACCOUNT)
            },
            onScanQr = {
                showMenuOverlay = false
                launchQrScanner()
            },
            onPrivacy = {
                showMenuOverlay = false
                tabNavController.navigate(ParentRoutes.PRIVACY)
            },
            onTerms = {
                showMenuOverlay = false
                tabNavController.navigate(ParentRoutes.TERMS)
            },
            onLogOut = {
                showMenuOverlay = false
                onLogOut()
            },
        )
    }
}
