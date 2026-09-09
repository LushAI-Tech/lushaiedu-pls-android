package com.lushaiedupls.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.remote.dto.OnboardingState
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.push.PushRouter
import com.lushaiedupls.ui.auth.google.rememberGoogleSignInAction
import com.lushaiedupls.ui.admin.AdminShell
import com.lushaiedupls.ui.auth.invitecode.InviteCodeRoute
import com.lushaiedupls.ui.auth.setupprofile.SetupProfileRoute
import com.lushaiedupls.ui.auth.selectclass.SelectClassRoute
import com.lushaiedupls.ui.auth.selectinstitution.SelectInstitutionRoute
import com.lushaiedupls.ui.auth.selectrole.SelectRoleRoute
import com.lushaiedupls.ui.auth.selectrole.SelectRoleViewModel
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.auth.selectsubject.SelectSubjectRoute
import com.lushaiedupls.ui.auth.signin.SignInRoute
import com.lushaiedupls.ui.auth.signup.CreateAccountRoute
import com.lushaiedupls.ui.auth.welcome.WelcomeRoute
import com.lushaiedupls.ui.common.ComingSoonScreen
import com.lushaiedupls.ui.parent.ParentShell
import com.lushaiedupls.ui.student.StudentShell
import com.lushaiedupls.ui.teacher.TeacherShell
import kotlinx.coroutines.launch

private val AppFadeRoutes = setOf(
    AppRoutes.WELCOME,
    AppRoutes.STUDENT_SHELL,
    AppRoutes.TEACHER_SHELL,
    AppRoutes.PARENT_SHELL,
    AppRoutes.ADMIN_SHELL,
    AppRoutes.COMING_SOON,
)

@Composable
fun AppNavGraph(
    userSessionStore: UserSessionStore,
    authRepository: AuthRepository,
    studentRepository: StudentRepository,
    teacherRepository: TeacherRepository,
    parentRepository: ParentRepository,
    adminRepository: AdminRepository,
    pushRouter: PushRouter,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = authRepository.routeForStoredSession(),
) {
    val scope = rememberCoroutineScope()

    fun navigateAfterAuth(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoutes.WELCOME) { inclusive = true }
        }
    }

    fun navigateToSignInWithMessage(message: String?) {
        val text = message?.takeIf { it.isNotBlank() } ?: return
        if (text.contains("cancelled", ignoreCase = true)) return
        authRepository.setPendingSignInMessage(text)
        navController.navigate(AppRoutes.SIGN_IN) {
            launchSingleTop = true
        }
    }

    val welcomeGoogleSignIn = rememberGoogleSignInAction(
        onIdToken = { token ->
            scope.launch {
                when (val result = authRepository.google(token)) {
                    is NetworkResult.Success -> {
                        navigateAfterAuth(
                            authRepository.resolvePostAuthRoute(
                                result.data,
                                fromGoogle = true,
                            ),
                        )
                    }
                    else -> navigateToSignInWithMessage(result.userMessage())
                }
            }
        },
        onError = { message -> navigateToSignInWithMessage(message) },
    )

    fun logOutToWelcome() {
        scope.launch {
            studentRepository.clearAiCache()
            teacherRepository.clearCaches()
            parentRepository.clearCaches()
            adminRepository.clearCaches()
            authRepository.logout()
            navController.navigate(AppRoutes.WELCOME) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        authRepository.sessionExpired.collect {
            studentRepository.clearAiCache()
            teacherRepository.clearCaches()
            parentRepository.clearCaches()
            adminRepository.clearCaches()
            authRepository.clearLocalSession()
            if (navController.currentDestination?.route != AppRoutes.WELCOME) {
                navController.navigate(AppRoutes.WELCOME) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    fun navigateBackFromOnboarding() {
        if (!navController.popBackStack()) {
            logOutToWelcome()
        }
    }

    LaunchedEffect(Unit) {
        if (!authRepository.isLoggedIn()) return@LaunchedEffect
        when (val result = authRepository.me()) {
            is NetworkResult.Success -> {
                val dest = authRepository.routeForUser(result.data)
                val current = navController.currentDestination?.route
                val wizard = setOf(
                    AppRoutes.SETUP_PROFILE,
                    AppRoutes.SELECT_ROLE,
                    AppRoutes.SELECT_INVITE_CODE,
                    AppRoutes.SELECT_INSTITUTION,
                    AppRoutes.SELECT_CLASS,
                    AppRoutes.SELECT_SUBJECT,
                )
                if (current == dest) return@LaunchedEffect
                // Don't pull users out of the onboarding wizard while still incomplete.
                if (current in wizard &&
                    result.data.onboarding_state != OnboardingState.COMPLETE
                ) {
                    return@LaunchedEffect
                }
                navController.navigate(dest) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { lushEnterTransition(fadeRoutes = AppFadeRoutes) },
        exitTransition = { lushExitTransition(fadeRoutes = AppFadeRoutes) },
        popEnterTransition = { lushPopEnterTransition(fadeRoutes = AppFadeRoutes) },
        popExitTransition = { lushPopExitTransition(fadeRoutes = AppFadeRoutes) },
    ) {
        composable(AppRoutes.WELCOME) {
            WelcomeRoute(
                onCreateAccount = { navController.navigate(AppRoutes.CREATE_ACCOUNT) },
                onSignIn = { navController.navigate(AppRoutes.SIGN_IN) },
                onGoogle = welcomeGoogleSignIn,
                onParent = {
                    userSessionStore.setParentSignupFlow(true)
                    navController.navigate(AppRoutes.CREATE_ACCOUNT)
                },
            )
        }
        composable(AppRoutes.SIGN_IN) {
            SignInRoute(
                authRepository = authRepository,
                onNavigate = { route -> navigateAfterAuth(route) },
                onSignUp = {
                    navController.navigate(AppRoutes.CREATE_ACCOUNT) {
                        popUpTo(AppRoutes.SIGN_IN) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.CREATE_ACCOUNT) {
            CreateAccountRoute(
                authRepository = authRepository,
                userSessionStore = userSessionStore,
                studentRepository = studentRepository,
                onNavigate = { route -> navigateAfterAuth(route) },
                onSignIn = {
                    navController.navigate(AppRoutes.SIGN_IN) {
                        popUpTo(AppRoutes.CREATE_ACCOUNT) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.SETUP_PROFILE) {
            SetupProfileRoute(
                authRepository = authRepository,
                studentRepository = studentRepository,
                userSessionStore = userSessionStore,
                onBack = { navigateBackFromOnboarding() },
                onContinue = { route ->
                    if (route == AppRoutes.SELECT_ROLE) {
                        navController.navigate(AppRoutes.SELECT_ROLE) {
                            popUpTo(AppRoutes.SETUP_PROFILE) { inclusive = true }
                        }
                    } else {
                        navigateAfterAuth(route)
                    }
                },
            )
        }
        composable(AppRoutes.SELECT_ROLE) {
            val roleViewModel: SelectRoleViewModel = viewModel(
                factory = SelectRoleViewModel.provideFactory(userSessionStore, authRepository),
            )
            SelectRoleRoute(
                viewModel = roleViewModel,
                onBack = { navigateBackFromOnboarding() },
                onContinueToClass = { navController.navigate(AppRoutes.SELECT_INSTITUTION) },
                onContinueToInvite = { navController.navigate(AppRoutes.SELECT_INVITE_CODE) },
                onFinished = { route -> navigateAfterAuth(route) },
            )
        }
        composable(AppRoutes.SELECT_INVITE_CODE) {
            InviteCodeRoute(
                userSessionStore = userSessionStore,
                authRepository = authRepository,
                onBack = { navigateBackFromOnboarding() },
                onContinueToClass = { navController.navigate(AppRoutes.SELECT_INSTITUTION) },
                onFinished = { route -> navigateAfterAuth(route) },
            )
        }
        composable(AppRoutes.SELECT_INSTITUTION) {
            SelectInstitutionRoute(
                userSessionStore = userSessionStore,
                studentRepository = studentRepository,
                onBack = { navigateBackFromOnboarding() },
                onContinue = { navController.navigate(AppRoutes.SELECT_CLASS) },
            )
        }
        composable(AppRoutes.SELECT_CLASS) {
            SelectClassRoute(
                userSessionStore = userSessionStore,
                studentRepository = studentRepository,
                onBack = { navigateBackFromOnboarding() },
                onContinue = { navController.navigate(AppRoutes.SELECT_SUBJECT) },
            )
        }
        composable(AppRoutes.SELECT_SUBJECT) {
            SelectSubjectRoute(
                userSessionStore = userSessionStore,
                studentRepository = studentRepository,
                authRepository = authRepository,
                onBack = { navigateBackFromOnboarding() },
                onDone = {
                    navigateAfterAuth(
                        authRepository.routeForStoredSession().takeIf {
                            it != AppRoutes.WELCOME
                        } ?: AppRoutes.STUDENT_SHELL,
                    )
                },
            )
        }
        composable(AppRoutes.STUDENT_SHELL) {
            StudentShell(
                userSessionStore = userSessionStore,
                studentRepository = studentRepository,
                authRepository = authRepository,
                onLogOut = { logOutToWelcome() },
            )
        }
        composable(AppRoutes.PARENT_SHELL) {
            ParentShell(
                userSessionStore = userSessionStore,
                parentRepository = parentRepository,
                studentRepository = studentRepository,
                authRepository = authRepository,
                onLogOut = { logOutToWelcome() },
            )
        }
        composable(AppRoutes.ADMIN_SHELL) {
            AdminShell(
                userSessionStore = userSessionStore,
                adminRepository = adminRepository,
                studentRepository = studentRepository,
                authRepository = authRepository,
                pushRouter = pushRouter,
                onLogOut = { logOutToWelcome() },
            )
        }
        composable(AppRoutes.TEACHER_SHELL) {
            TeacherShell(
                userSessionStore = userSessionStore,
                teacherRepository = teacherRepository,
                studentRepository = studentRepository,
                authRepository = authRepository,
                onLogOut = { logOutToWelcome() },
                // Switch Roles — re-enable later.
                // onSwitchRole = { role ->
                //     userSessionStore.setRole(role)
                //     val dest = when (role) {
                //         UserRole.Student -> AppRoutes.STUDENT_SHELL
                //         UserRole.Teacher -> AppRoutes.TEACHER_SHELL
                //         UserRole.Parents -> AppRoutes.PARENT_SHELL
                //         UserRole.Admin -> AppRoutes.ADMIN_SHELL
                //     }
                //     navController.navigate(dest) {
                //         popUpTo(0) { inclusive = true }
                //     }
                // },
            )
        }
        composable(AppRoutes.COMING_SOON) {
            val label = when (userSessionStore.getRole()) {
                UserRole.Admin -> "Admin"
                UserRole.Parents -> "Parents"
                else -> "This role"
            }
            ComingSoonScreen(
                roleLabel = label,
                onLogOut = { logOutToWelcome() },
            )
        }
    }
}
