package com.lushaiedupls.data.remote.device

import android.content.Context
import android.os.Build
import com.lushaiedupls.data.remote.dto.DeviceInfo
import com.lushaiedupls.data.remote.dto.DevicePlatform
import java.util.UUID
import kotlinx.coroutines.withTimeoutOrNull

class DeviceIdProvider(
    context: Context,
    private val appVersion: String,
    private val fcmTokenSource: FcmTokenSource = FcmTokenSource { null },
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    fun cachedFcmToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun cacheFcmToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        prefs.edit().putString(KEY_FCM_TOKEN, trimmed).apply()
    }

    fun markFcmTokenUploaded(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        prefs.edit()
            .putString(KEY_FCM_TOKEN, trimmed)
            .putString(KEY_FCM_TOKEN_UPLOADED, trimmed)
            .apply()
    }

    fun uploadedFcmToken(): String? =
        prefs.getString(KEY_FCM_TOKEN_UPLOADED, null)?.takeIf { it.isNotBlank() }

    /**
     * Fetches and caches a live FCM token when possible. Login can then send
     * [DeviceInfo.fcm_token] even if the in-request fetch is slow or fails.
     */
    suspend fun prefetchFcmToken(): String? = refreshFcmToken()

    /**
     * Builds the auth `device` payload. Prefers a live FCM token, then the
     * cached one. Omits [DeviceInfo.fcm_token] only when none is available so
     * a re-login does not wipe a previously registered token.
     */
    suspend fun deviceInfo(): DeviceInfo = snapshot(fcmToken = refreshFcmToken())

    fun snapshot(fcmToken: String? = cachedFcmToken()): DeviceInfo = DeviceInfo(
        device_id = deviceId(),
        platform = DevicePlatform.ANDROID,
        device_name = DeviceProfile.deviceName(Build.MANUFACTURER, Build.MODEL),
        user_agent = DeviceProfile.userAgent(
            appVersion = appVersion,
            androidRelease = Build.VERSION.RELEASE,
            model = Build.MODEL,
            sdkInt = Build.VERSION.SDK_INT,
        ),
        fcm_token = resolveFcmToken(live = fcmToken, cached = null),
    )

    private suspend fun refreshFcmToken(): String? {
        val cached = cachedFcmToken()
        val live = runCatching {
            withTimeoutOrNull(FCM_TIMEOUT_MS) { fcmTokenSource.currentToken() }
        }.getOrNull()
        val token = resolveFcmToken(live = live, cached = cached)
        if (token != null && token != cached) cacheFcmToken(token)
        return token
    }

    companion object {
        private const val PREFS = "lushai_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_TOKEN_UPLOADED = "fcm_token_uploaded"
        private const val FCM_TIMEOUT_MS = 8_000L

        internal fun resolveFcmToken(live: String?, cached: String?): String? =
            live?.trim()?.takeIf { it.isNotEmpty() }
                ?: cached?.trim()?.takeIf { it.isNotEmpty() }
    }
}
