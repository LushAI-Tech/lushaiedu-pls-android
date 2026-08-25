package com.lushaiedupls.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.admin.announcements.AdminAnnouncementsRoute
import com.lushaiedupls.ui.admin.calendar.AdminCalendarRoute
import com.lushaiedupls.ui.admin.classes.AdminClassesRoute
import com.lushaiedupls.ui.admin.feedback.AdminFeedbackRoute
import com.lushaiedupls.ui.admin.fees.AdminFeesRoute
import com.lushaiedupls.ui.admin.home.AdminHomeRoute
import com.lushaiedupls.ui.admin.invites.AdminInvitesRoute
import com.lushaiedupls.ui.admin.menu.AdminMenuOverlay
import com.lushaiedupls.ui.admin.more.AdminMoreScreen
import com.lushaiedupls.ui.admin.notifications.AdminNotificationsRoute
import com.lushaiedupls.ui.admin.periods.AdminPeriodsRoute
import com.lushaiedupls.ui.admin.users.AdminUsersRoute
import com.lushaiedupls.ui.navigation.lushEnterTransition
import com.lushaiedupls.ui.navigation.lushExitTransition
import com.lushaiedupls.ui.navigation.lushPopEnterTransition
import com.lushaiedupls.ui.navigation.lushPopExitTransition
import com.lushaiedupls.ui.student.menu.LegalDocumentScreen
import com.lushaiedupls.ui.student.menu.StudentAccountRoute
import com.lushaiedupls.ui.theme.BgWhite

private val AdminTabRoutes = setOf(
    AdminRoutes.HOME,
    AdminRoutes.USERS,
    AdminRoutes.CLASSES,
    AdminRoutes.MORE,
)

@Composable
fun AdminShell(
    userSessionStore: UserSessionStore,
    adminRepository: AdminRepository,
    studentRepository: StudentRepository,
    authRepository: AuthRepository,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showMenuOverlay by remember { mutableStateOf(false) }

    val inMoreStack = currentRoute in setOf(
        AdminRoutes.MORE,
        AdminRoutes.FEES,
        AdminRoutes.FEEDBACK,
        AdminRoutes.INVITES,
        AdminRoutes.PERIODS,
        AdminRoutes.CALENDAR,
        AdminRoutes.ANNOUNCEMENTS,
    )
    val selectedTab = when {
        currentRoute == AdminRoutes.USERS -> AdminTab.Users
        currentRoute == AdminRoutes.CLASSES -> AdminTab.Classes
        inMoreStack -> AdminTab.More
        else -> AdminTab.Home
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding(),
        containerColor = BgWhite,
        bottomBar = {
            AdminBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> navigateTab(tab.route) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = AdminRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { lushEnterTransition(tabRoutes = AdminTabRoutes) },
            exitTransition = { lushExitTransition(tabRoutes = AdminTabRoutes) },
            popEnterTransition = { lushPopEnterTransition(tabRoutes = AdminTabRoutes) },
            popExitTransition = { lushPopExitTransition(tabRoutes = AdminTabRoutes) },
        ) {
            composable(AdminRoutes.HOME) {
                AdminHomeRoute(
                    userSessionStore = userSessionStore,
                    adminRepository = adminRepository,
                    onNotificationsClick = { tabNavController.navigate(AdminRoutes.NOTIFICATIONS) },
                    onProfileClick = { showMenuOverlay = true },
                )
            }
            composable(AdminRoutes.USERS) {
                AdminUsersRoute(
                    adminRepository = adminRepository,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.CLASSES) {
                AdminClassesRoute(
                    adminRepository = adminRepository,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.MORE) {
                AdminMoreScreen(
                    onFees = { tabNavController.navigate(AdminRoutes.FEES) },
                    onFeedback = { tabNavController.navigate(AdminRoutes.FEEDBACK) },
                    onInvites = { tabNavController.navigate(AdminRoutes.INVITES) },
                    onPeriods = { tabNavController.navigate(AdminRoutes.PERIODS) },
                    onCalendar = { tabNavController.navigate(AdminRoutes.CALENDAR) },
                    onAnnouncements = { tabNavController.navigate(AdminRoutes.ANNOUNCEMENTS) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.FEES) {
                AdminFeesRoute(
                    adminRepository = adminRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.FEEDBACK) {
                AdminFeedbackRoute(
                    adminRepository = adminRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.INVITES) {
                AdminInvitesRoute(
                    adminRepository = adminRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.PERIODS) {
                AdminPeriodsRoute(
                    adminRepository = adminRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.CALENDAR) {
                AdminCalendarRoute(
                    adminRepository = adminRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.ANNOUNCEMENTS) {
                AdminAnnouncementsRoute(
                    adminRepository = adminRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.NOTIFICATIONS) {
                AdminNotificationsRoute(
                    adminRepository = adminRepository,
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.ACCOUNT) {
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
            composable(AdminRoutes.PRIVACY) {
                LegalDocumentScreen(
                    title = stringResource(R.string.privacy_title),
                    body = stringResource(R.string.privacy_body),
                    onBack = { tabNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(AdminRoutes.TERMS) {
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
        AdminMenuOverlay(
            onDismiss = { showMenuOverlay = false },
            onAccount = {
                showMenuOverlay = false
                tabNavController.navigate(AdminRoutes.ACCOUNT)
            },
            onPrivacy = {
                showMenuOverlay = false
                tabNavController.navigate(AdminRoutes.PRIVACY)
            },
            onTerms = {
                showMenuOverlay = false
                tabNavController.navigate(AdminRoutes.TERMS)
            },
            onLogOut = {
                showMenuOverlay = false
                onLogOut()
            },
        )
    }
}
