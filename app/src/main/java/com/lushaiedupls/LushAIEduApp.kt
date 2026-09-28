package com.lushaiedupls

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.lushaiedupls.data.remote.ApiHttpLogger
import com.lushaiedupls.di.AppContainer
import com.lushaiedupls.push.PushNotificationHelper
import com.lushaiedupls.ui.common.markdown.KatexRenderer
import com.lushaiedupls.ui.theme.applyApplicationLightMode
import com.lushaiedupls.ui.theme.withForcedLightMode

class LushAIEduApp : Application() {

    lateinit var container: AppContainer
        private set

    val isContainerReady: Boolean
        get() = this::container.isInitialized

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withForcedLightMode())
    }

    override fun onCreate() {
        super.onCreate()
        applyApplicationLightMode()
        ApiHttpLogger.log(
            "App started v${BuildConfig.VERSION_NAME} " +
                "debug=${BuildConfig.DEBUG} apiLogs=${BuildConfig.ENABLE_API_LOGS} " +
                "googleOauthLogs=${BuildConfig.ENABLE_GOOGLE_OAUTH_LOGS} " +
                "base=${BuildConfig.BASE_URL}",
        )
        PushNotificationHelper.ensureChannel(this)
        container = AppContainer(this)
        container.pushTokenSynchronizer.prefetchAndSync()
        Handler(Looper.getMainLooper()).post { KatexRenderer.prewarm(this) }
    }
}
