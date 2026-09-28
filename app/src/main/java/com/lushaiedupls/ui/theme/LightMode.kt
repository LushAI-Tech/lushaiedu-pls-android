package com.lushaiedupls.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build

/**
 * This app is light-only. Keep the process / activity configuration on night-no
 * so system dark mode cannot restyle resources or trigger force-dark.
 */
fun Context.withForcedLightMode(): Context {
    val config = Configuration(resources.configuration)
    val nightBits = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (nightBits == Configuration.UI_MODE_NIGHT_NO) return this
    config.uiMode =
        (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
    return createConfigurationContext(config)
}

fun Context.applyApplicationLightMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(UiModeManager::class.java)
            ?.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
    }
}
