package com.lushaiedupls.data.remote.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceIdProviderFcmTokenTest {

    @Test
    fun resolveFcmToken_prefersLiveToken() {
        assertEquals(
            "live-token",
            DeviceIdProvider.resolveFcmToken(live = "live-token", cached = "cached-token"),
        )
    }

    @Test
    fun resolveFcmToken_fallsBackToCachedWhenLiveMissing() {
        assertEquals(
            "cached-token",
            DeviceIdProvider.resolveFcmToken(live = null, cached = "cached-token"),
        )
        assertEquals(
            "cached-token",
            DeviceIdProvider.resolveFcmToken(live = "  ", cached = "cached-token"),
        )
    }

    @Test
    fun resolveFcmToken_returnsNullWhenNothingAvailable() {
        assertNull(DeviceIdProvider.resolveFcmToken(live = null, cached = null))
        assertNull(DeviceIdProvider.resolveFcmToken(live = " ", cached = ""))
    }
}
