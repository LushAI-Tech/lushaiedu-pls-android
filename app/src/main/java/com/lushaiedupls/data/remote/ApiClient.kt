package com.lushaiedupls.data.remote

import com.lushaiedupls.data.remote.device.DeviceIdProvider
import com.lushaiedupls.data.remote.interceptors.ApiBodyLoggingInterceptor
import com.lushaiedupls.data.remote.interceptors.AuthInterceptor
import com.lushaiedupls.data.remote.interceptors.HeaderInterceptor
import com.lushaiedupls.data.remote.interceptors.LongTimeoutInterceptor
import com.lushaiedupls.data.remote.interceptors.TokenRefreshAuthenticator
import com.lushaiedupls.data.remote.token.TokenProvider
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun createOkHttpClient(
        tokenProvider: TokenProvider,
        deviceIdProvider: DeviceIdProvider,
        appVersion: String,
        isDebug: Boolean,
        onUnauthorized: () -> Unit = {},
    ): OkHttpClient {
        val refreshClient = baseClientBuilder(appVersion, isDebug).build()

        return baseClientBuilder(appVersion, isDebug)
            .addInterceptor(AuthInterceptor(tokenProvider))
            .authenticator(
                TokenRefreshAuthenticator(
                    tokenProvider = tokenProvider,
                    deviceIdProvider = deviceIdProvider,
                    json = json,
                    refreshClient = refreshClient,
                    onRefreshFailed = onUnauthorized,
                ),
            )
            .build()
    }

    fun createRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        val contentType = ApiConfig.CONTENT_TYPE_JSON.toMediaType()
        return Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(baseUrl))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun create(
        baseUrl: String,
        tokenProvider: TokenProvider,
        deviceIdProvider: DeviceIdProvider,
        appVersion: String,
        isDebug: Boolean,
        onUnauthorized: () -> Unit = {},
    ): Retrofit = createRetrofit(
        baseUrl = baseUrl,
        okHttpClient = createOkHttpClient(
            tokenProvider = tokenProvider,
            deviceIdProvider = deviceIdProvider,
            appVersion = appVersion,
            isDebug = isDebug,
            onUnauthorized = onUnauthorized,
        ),
    )

    inline fun <reified T> createService(retrofit: Retrofit): T = retrofit.create(T::class.java)

    private fun baseClientBuilder(appVersion: String, isDebug: Boolean): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(appVersion))
            .addInterceptor(LongTimeoutInterceptor())

        if (isDebug) {
            val headerLogging = HttpLoggingInterceptor(ApiHttpLogger).apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader(ApiConfig.HEADER_AUTHORIZATION)
            }
            builder.addInterceptor(headerLogging)
            builder.addInterceptor(ApiBodyLoggingInterceptor())
        }

        return builder
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
