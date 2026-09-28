package com.lushaiedupls.data.remote

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * OkHttp logger. Filter Logcat by tag [TAG] (`LushApi`).
 *
 * Uses ERROR on purpose: Vivo/Oppo/ColorOS hide app INFO/DEBUG, and R8 strips Log.d.
 * Staging/debug only — interceptors are not installed when [BuildConfig.ENABLE_API_LOGS] is false.
 */
object ApiHttpLogger : HttpLoggingInterceptor.Logger {
    const val TAG = "LushApi"

    private const val MAX_LOG_CHUNK = 3500

    override fun log(message: String) {
        if (message.length <= MAX_LOG_CHUNK) {
            Log.println(Log.ERROR, TAG, message)
            return
        }
        var start = 0
        while (start < message.length) {
            val end = minOf(start + MAX_LOG_CHUNK, message.length)
            Log.println(Log.ERROR, TAG, message.substring(start, end))
            start = end
        }
    }
}
