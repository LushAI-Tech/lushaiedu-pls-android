package com.lushaiedupls.data.remote

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug-only OkHttp logger. Filter Logcat by tag [TAG] (`LushApi`) to see:
 * - HTTP headers (Authorization redacted)
 * - Pretty-printed REQUEST BODY / RESPONSE BODY blocks
 */
object ApiHttpLogger : HttpLoggingInterceptor.Logger {
    const val TAG = "LushApi"

    private const val MAX_LOG_CHUNK = 3500

    override fun log(message: String) {
        if (message.length <= MAX_LOG_CHUNK) {
            Log.d(TAG, message)
            return
        }
        var start = 0
        while (start < message.length) {
            val end = minOf(start + MAX_LOG_CHUNK, message.length)
            Log.d(TAG, message.substring(start, end))
            start = end
        }
    }
}
