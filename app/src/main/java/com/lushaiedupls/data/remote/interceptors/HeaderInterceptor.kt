package com.lushaiedupls.data.remote.interceptors

import com.lushaiedupls.data.remote.ApiConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

/**
 * Adds headers that every request should carry (accept, platform, app version, language).
 */
class HeaderInterceptor(
    private val appVersion: String,
    private val languageProvider: () -> String = { Locale.getDefault().toLanguageTag() },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(ApiConfig.HEADER_ACCEPT, ApiConfig.CONTENT_TYPE_JSON)
            .header(ApiConfig.HEADER_PLATFORM, ApiConfig.PLATFORM_ANDROID)
            .header(ApiConfig.HEADER_APP_VERSION, appVersion)
            .header(ApiConfig.HEADER_ACCEPT_LANGUAGE, languageProvider())
            .build()

        return chain.proceed(request)
    }
}
