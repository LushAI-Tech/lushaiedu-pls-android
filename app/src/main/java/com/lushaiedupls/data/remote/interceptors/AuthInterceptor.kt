package com.lushaiedupls.data.remote.interceptors

import com.lushaiedupls.data.remote.ApiConfig
import com.lushaiedupls.data.remote.token.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches a Bearer access token when one is stored.
 * Public auth calls (login / register / google / refresh) go out without Authorization
 * so refresh is not sent with an expired access token.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (AuthPaths.isPublicAuth(original.url.encodedPath)) {
            return chain.proceed(original)
        }

        val token = tokenProvider.getAccessToken()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header(ApiConfig.HEADER_AUTHORIZATION, "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
