package com.lushaiedupls.data.remote.device

import android.content.Context
import android.os.Build
import com.lushaiedupls.data.remote.dto.DeviceInfo
import com.lushaiedupls.data.remote.dto.DevicePlatform
import java.util.UUID

class DeviceIdProvider(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    fun deviceInfo(fcmToken: String? = null): DeviceInfo = DeviceInfo(
        device_id = deviceId(),
        platform = DevicePlatform.ANDROID,
        device_name = Build.MODEL,
        user_agent = "LushAIEdu_PLS/${Build.VERSION.RELEASE}",
        fcm_token = fcmToken,
    )

    companion object {
        private const val PREFS = "lushai_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
