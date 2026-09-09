package com.lushaiedupls.push

import com.lushaiedupls.data.remote.api.MeApi
import com.lushaiedupls.data.remote.device.DeviceIdProvider
import com.lushaiedupls.data.remote.dto.FcmTokenUpdateRequest
import com.lushaiedupls.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PushTokenSynchronizer(
    private val meApi: MeApi,
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionRepository: SessionRepository,
    private val scope: CoroutineScope,
) {
    fun onNewToken(token: String) {
        deviceIdProvider.cacheFcmToken(token)
        syncAsync()
    }

    fun prefetchAndSync() {
        scope.launch {
            runCatching { deviceIdProvider.prefetchFcmToken() }
            if (sessionRepository.isLoggedIn()) {
                runCatching { sync() }
            }
        }
    }

    fun syncAsync() {
        if (!sessionRepository.isLoggedIn()) return
        scope.launch {
            runCatching { sync() }
        }
    }

    suspend fun sync() {
        if (!sessionRepository.isLoggedIn()) return
        val token = deviceIdProvider.cachedFcmToken() ?: return
        if (token == deviceIdProvider.uploadedFcmToken()) return
        val localId = deviceIdProvider.deviceId()
        val devices = meApi.devices(localId)
        val row = devices.firstOrNull { it.is_current }
            ?: devices.firstOrNull { it.device_id == localId }
            ?: return
        meApi.updateFcmToken(row.id, FcmTokenUpdateRequest(fcm_token = token))
        deviceIdProvider.markFcmTokenUploaded(token)
    }
}
