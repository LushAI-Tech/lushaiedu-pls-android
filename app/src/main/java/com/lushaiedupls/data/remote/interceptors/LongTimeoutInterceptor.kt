package com.lushaiedupls.data.remote.interceptors

import com.lushaiedupls.data.remote.ApiConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Quiz grading and AI chat generate text on the server and routinely exceed the
 * default 30s read timeout. Stretch the wait only for those paths.
 */
class LongTimeoutInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        if (!needsLongWait(path)) return chain.proceed(chain.request())
        return chain
            .withReadTimeout(ApiConfig.LONG_READ_TIMEOUT_SECONDS.toInt(), TimeUnit.SECONDS)
            .withWriteTimeout(ApiConfig.LONG_WRITE_TIMEOUT_SECONDS.toInt(), TimeUnit.SECONDS)
            .proceed(chain.request())
    }

    private fun needsLongWait(path: String): Boolean =
        LONG_WAIT_MARKERS.any { marker -> marker in path }

    companion object {
        private val LONG_WAIT_MARKERS = listOf(
            "/api/v1/ai/quiz/submit",
            "/api/v1/ai/quiz/chapter/",
            "/api/v1/ai/quiz/section/",
            "/api/v1/ai/chat/",
        )
    }
}
