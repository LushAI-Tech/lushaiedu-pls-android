package com.lushaiedupls

import android.app.Application
import com.lushaiedupls.di.AppContainer

class LushAIEduApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
