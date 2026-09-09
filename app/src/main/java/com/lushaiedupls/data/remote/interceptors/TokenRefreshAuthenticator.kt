package com.lushaiedupls.data.remote.interceptors

import android.util.Log
import com.lushaiedupls.data.remote.ApiConfig
import com.lushaiedupls.data.remote.ApiHttpLogger
import com.lushaiedupls.data.remote.dto.RefreshRequest
import com.lushaiedupls.data.remote.token.TokenProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * On 401, calls `/auth/refresh` and retries the original request up to [MAX_REFRESH_RETRIES]
 * times. Concurrent 401s share a single refresh. If the request is still 401 after that,
 * the session is cleared and the user is logged out.
 */
class TokenRefreshAuthenticator(
    private val tokenProvider: TokenProvider,
    private val deviceId: () -> String,
    private val json: Json,
    private val refreshClient: OkHttpClient,
    private val onRefreshFailed: () -> Unit,
) : Authenticator {

    private val lock = Any()
    private val sessionExpired = AtomicBoolean(false)

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != HTTP_UNAUTHORIZED) return null
        if (AuthPaths.isPublicAuth(response.request.url.encodedPath)) return null
        if (sessionExpired.get()) {
            if (tokenProvider.getRefreshToken().isNullOrBlank()) return null
            sessionExpired.set(false)
        }

        val attempt = unauthorizedCount(response)
        if (attempt > MAX_REFRESH_RETRIES) {
            Log.w(
                ApiHttpLogger.TAG,
                "Still 401 after $MAX_REFRESH_RETRIES refresh retries; logging out",
            )
            expireSession()
            return null
        }

        val failedAccess = bearerOf(response.request)
        val newAccess = synchronized(lock) {
            if (sessionExpired.get()) return@synchronized null
            val currentAccess = tokenProvider.getAccessToken()
            if (!currentAccess.isNullOrBlank() && currentAccess != failedAccess) {
                currentAccess
            } else {
                val refreshToken = tokenProvider.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    expireSession()
                    null
                } else {
                    refreshAccessToken(
                        refreshToken = refreshToken,
                        failedRequestUrl = response.request.url,
                    )
                }
            }
        } ?: return null

        return response.request.newBuilder()
            .header(ApiConfig.HEADER_AUTHORIZATION, "Bearer $newAccess")
            .build()
    }

    private fun refreshAccessToken(refreshToken: String, failedRequestUrl: HttpUrl): String? {
        return try {
            val body = json.encodeToString(
                RefreshRequest(
                    refresh_token = refreshToken,
                    device_id = deviceId(),
                ),
            )
            val request = Request.Builder()
                .url(refreshUrl(failedRequestUrl))
                .post(body.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            refreshClient.newCall(request).execute().use { refreshResponse ->
                val payload = refreshResponse.body?.string().orEmpty()
                if (!refreshResponse.isSuccessful) {
                    Log.w(
                        ApiHttpLogger.TAG,
                        "Token refresh failed: HTTP ${refreshResponse.code} $payload",
                    )
                    if (refreshResponse.code == HTTP_UNAUTHORIZED ||
                        refreshResponse.code == HTTP_FORBIDDEN
                    ) {
                        expireSession()
                    }
                    return null
                }
                val tokens = parseTokenPair(payload) ?: run {
                    Log.w(ApiHttpLogger.TAG, "Token refresh returned an unreadable payload")
                    return null
                }
                tokenProvider.saveTokens(tokens.first, tokens.second)
                sessionExpired.set(false)
                Log.d(ApiHttpLogger.TAG, "Token refresh succeeded; retrying original request")
                tokens.first
            }
        } catch (e: Exception) {
            Log.w(ApiHttpLogger.TAG, "Token refresh error: ${e.message}")
            null
        }
    }

    private fun parseTokenPair(payload: String): Pair<String, String>? {
        return try {
            val obj = json.parseToJsonElement(payload).jsonObject
            val access = obj["access_token"]?.jsonPrimitive?.content?.trim().orEmpty()
            val refresh = obj["refresh_token"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (access.isBlank() || refresh.isBlank()) null else access to refresh
        } catch (_: Exception) {
            null
        }
    }

    private fun expireSession() {
        if (sessionExpired.compareAndSet(false, true)) {
            onRefreshFailed()
        }
    }

    private companion object {
        const val MAX_REFRESH_RETRIES = 3
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        val JSON_MEDIA_TYPE = ApiConfig.CONTENT_TYPE_JSON.toMediaType()

        fun refreshUrl(failedRequestUrl: HttpUrl): HttpUrl =
            failedRequestUrl.newBuilder()
                .encodedPath(ApiConfig.AUTH_REFRESH_PATH)
                .encodedQuery(null)
                .fragment(null)
                .build()

        fun bearerOf(request: Request): String? {
            val header = request.header(ApiConfig.HEADER_AUTHORIZATION) ?: return null
            return header.removePrefix("Bearer ").trim().takeIf { it.isNotBlank() }
        }

        fun unauthorizedCount(response: Response): Int {
            var count = 0
            var current: Response? = response
            while (current != null) {
                if (current.code == HTTP_UNAUTHORIZED) count++
                current = current.priorResponse
            }
            return count
        }
    }
}
