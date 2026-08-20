package com.lushaiedupls.data.remote.interceptors

import android.util.Log
import com.lushaiedupls.data.remote.ApiConfig
import com.lushaiedupls.data.remote.ApiHttpLogger
import com.lushaiedupls.data.remote.device.DeviceIdProvider
import com.lushaiedupls.data.remote.dto.RefreshRequest
import com.lushaiedupls.data.remote.dto.TokenPair
import com.lushaiedupls.data.remote.token.TokenProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * On 401, exchanges the stored refresh token (+ device_id) for a new token pair
 * and retries the original request. Concurrent 401s share a single refresh.
 */
class TokenRefreshAuthenticator(
    private val tokenProvider: TokenProvider,
    private val deviceIdProvider: DeviceIdProvider,
    private val json: Json,
    private val refreshClient: OkHttpClient,
    private val onRefreshFailed: () -> Unit,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_RETRIES) return null
        if (AuthPaths.isRefresh(response.request.url.encodedPath)) return null

        val failedAccess = bearerOf(response.request)

        val newAccess = synchronized(lock) {
            val currentAccess = tokenProvider.getAccessToken()
            if (!currentAccess.isNullOrBlank() && currentAccess != failedAccess) {
                currentAccess
            } else {
                val refreshToken = tokenProvider.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    if (!failedAccess.isNullOrBlank()) onRefreshFailed()
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
                    device_id = deviceIdProvider.deviceId(),
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
                        onRefreshFailed()
                    }
                    return null
                }
                val pair = json.decodeFromString<TokenPair>(payload)
                tokenProvider.saveTokens(pair.access_token, pair.refresh_token)
                Log.d(ApiHttpLogger.TAG, "Token refresh succeeded; retrying original request")
                pair.access_token
            }
        } catch (e: Exception) {
            Log.w(ApiHttpLogger.TAG, "Token refresh error: ${e.message}")
            null
        }
    }

    private companion object {
        const val MAX_RETRIES = 2
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

        fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }
}
