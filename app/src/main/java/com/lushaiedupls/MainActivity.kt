package com.lushaiedupls

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lushaiedupls.push.PushAction
import com.lushaiedupls.push.PushActions
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.common.KeyboardDismissState
import com.lushaiedupls.ui.common.LocalKeyboardDismiss
import com.lushaiedupls.ui.common.dismissKeyboardOnOutsideTap
import com.lushaiedupls.ui.navigation.AppNavGraph
import com.lushaiedupls.ui.splash.BrandAnchorState
import com.lushaiedupls.ui.splash.LocalBrandAnchors
import com.lushaiedupls.ui.splash.LushSplashScreen
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.withForcedLightMode
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    private val splashHandoffReady = AtomicBoolean(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withForcedLightMode())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !splashHandoffReady.get() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val container = (application as LushAIEduApp).container
        handlePushIntent(intent)
        setContent {
            LushAIEdu_PLSTheme {
                val brandAnchors = remember { BrandAnchorState() }
                var showSplash by rememberSaveable { mutableStateOf(true) }

                val keyboardDismiss = remember { KeyboardDismissState() }
                CompositionLocalProvider(
                    LocalBrandAnchors provides brandAnchors,
                    LocalKeyboardDismiss provides keyboardDismiss,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .dismissKeyboardOnOutsideTap(keyboardDismiss),
                    ) {
                        AppNavGraph(
                            userSessionStore = container.userSessionStore,
                            authRepository = container.authRepository,
                            studentRepository = container.studentRepository,
                            teacherRepository = container.teacherRepository,
                            parentRepository = container.parentRepository,
                            adminRepository = container.adminRepository,
                            pushRouter = container.pushRouter,
                        )
                        if (showSplash) {
                            LushSplashScreen(
                                onSystemSplashReady = {
                                    splashHandoffReady.set(true)
                                },
                                onFinished = {
                                    showSplash = false
                                    requestNotificationPermissionIfNeeded()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        val action = PushActions.fromIntent(intent) ?: return
        if (action !is PushAction.PendingApproval) return
        val container = (application as LushAIEduApp).container
        if (container.userSessionStore.getRole() != UserRole.Admin) return
        container.pushRouter.offer(action)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
