package com.lushaiedupls.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.PendingSignInNotice
import com.lushaiedupls.data.remote.deviceSessionConflictOrNull
import com.lushaiedupls.data.remote.loginUserMessage
import com.lushaiedupls.data.remote.dto.OnboardingState
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.push.PushRouter
import com.lushaiedupls.ui.auth.google.GoogleOauthLogger
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
    var welcomeOauthStatus by remember { mutableStateOf<String?>(null) }
    var welcomeOauthIsError by remember { mutableStateOf(false) }

    fun navigateAfterAuth(route: String) {
        welcomeOauthStatus = null
        welcomeOauthIsError = false
        navController.navigate(route) {
            popUpTo(AppRoutes.WELCOME) { inclusive = true }
        }
    }

    fun showWelcomeOauthStatus(message: String?, isError: Boolean = false) {
        val text = message?.takeIf { it.isNotBlank() } ?: return
        if (!GoogleOauthLogger.enabled && !isError) return
        if (!GoogleOauthLogger.enabled && text.contains("cancelled", ignoreCase = true)) return
        welcomeOauthStatus = text
        welcomeOauthIsError = isError
    }

    fun navigateToSignInWithMessage(message: String?) {
        val text = message?.takeIf { it.isNotBlank() } ?: return
        if (GoogleOauthLogger.enabled) {
            showWelcomeOauthStatus(text, isError = true)
            return
        }
        if (text.contains("cancelled", ignoreCase = true)) return
        authRepository.setPendingSignInMessage(text)
        navController.navigate(AppRoutes.SIGN_IN) {
            launchSingleTop = true
        }
    }

    val welcomeGoogleSignIn = rememberGoogleSignInAction(
        onIdToken = { token ->
            scope.launch {
                // Keep prior activity logs — do not clear here.
                showWelcomeOauthStatus("Account selected. Calling auth/google…", isError = false)
                when (val result = authRepository.google(token)) {
                    is NetworkResult.Success -> {
                        showWelcomeOauthStatus("Signed in. Opening app…", isError = false)
                        navigateAfterAuth(
                            authRepository.resolvePostAuthRoute(
                                result.data,
                                fromGoogle = true,
                            ),
                        )
                    }
                    else -> {
                        val conflict = result.deviceSessionConflictOrNull()
                        if (conflict != null) {
                            authRepository.setPendingSignInNotice(
                                PendingSignInNotice(
                                    message = conflict.message,
                                    conflict = conflict,
                                    googleIdToken = token,
                                ),
                            )
                            navController.navigate(AppRoutes.SIGN_IN) {
                                launchSingleTop = true
                            }
                        } else {
                            navigateToSignInWithMessage(
                                GoogleOauthLogger.uiNetworkMessage(
                                    result.loginUserMessage(),
                                    result,
                                ),
                            )
                        }
                    }
                }
            }
        },
        onError = { message -> navigateToSignInWithMessage(message) },
        onStatus = { status -> showWelcomeOauthStatus(status, isError = false) },
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
                if ((current in wizard ||
                        current?.startsWith("select_class/") == true ||
                        current?.startsWith("select_subject/") == true) &&
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
                onCreateAccount = {
                    welcomeOauthStatus = null
                    welcomeOauthIsError = false
                    navController.navigate(AppRoutes.CREATE_ACCOUNT)
                },
                onSignIn = {
                    welcomeOauthStatus = null
                    welcomeOauthIsError = false
                    navController.navigate(AppRoutes.SIGN_IN)
                },
                onGoogle = {
                    welcomeOauthStatus = if (GoogleOauthLogger.enabled) {
                        "Starting Google sign-in…"
                    } else {
                        null
                    }
                    welcomeOauthIsError = false
                    welcomeGoogleSignIn()
                },
                onParent = {
                    welcomeOauthStatus = null
                    welcomeOauthIsError = false
                    userSessionStore.setParentSignupFlow(true)
                    navController.navigate(AppRoutes.CREATE_ACCOUNT)
                },
                oauthStatusMessage = welcomeOauthStatus,
                oauthStatusIsError = welcomeOauthIsError,
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
                onContinue = {
                    val institutionId = userSessionStore.getInstitutionIds().firstOrNull()
                    if (institutionId != null) {
                        navController.navigate(AppRoutes.selectClass(institutionId))
                    }
                },
            )
        }
        composable(AppRoutes.SELECT_CLASS) { entry ->
            val institutionId = entry.arguments?.getString("institutionId").orEmpty()
            SelectClassRoute(
                userSessionStore = userSessionStore,
                studentRepository = studentRepository,
                institutionId = institutionId,
                onBack = { navigateBackFromOnboarding() },
                onContinue = {
                    navController.navigate(AppRoutes.selectSubject(institutionId))
                },
            )
        }
        composable(AppRoutes.SELECT_SUBJECT) { entry ->
            val institutionId = entry.arguments?.getString("institutionId").orEmpty()
            SelectSubjectRoute(
                userSessionStore = userSessionStore,
                studentRepository = studentRepository,
                authRepository = authRepository,
                institutionId = institutionId,
                onBack = { navigateBackFromOnboarding() },
                onContinueToNextInstitution = { nextId ->
                    navController.navigate(AppRoutes.selectClass(nextId))
                },
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
