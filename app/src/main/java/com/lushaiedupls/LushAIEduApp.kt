package com.lushaiedupls

import android.app.Application
import com.lushaiedupls.di.AppContainer
import com.lushaiedupls.push.PushNotificationHelper

class LushAIEduApp : Application() {

    lateinit var container: AppContainer
        private set

    val isContainerReady: Boolean
        get() = this::container.isInitialized

    override fun onCreate() {
        super.onCreate()
        PushNotificationHelper.ensureChannel(this)
        container = AppContainer(this)
        container.pushTokenSynchronizer.prefetchAndSync()
    }
}
