package com.lushaiedupls.data.remote.device

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceProfileTest {

    @Test
    fun deviceName_usesModelWhenItAlreadyContainsManufacturer() {
        assertEquals("Google Pixel 8", DeviceProfile.deviceName("Google", "Google Pixel 8"))
    }

    @Test
    fun deviceName_prefixesManufacturerWhenMissingFromModel() {
        assertEquals("Google Pixel 8", DeviceProfile.deviceName("Google", "Pixel 8"))
        assertEquals("Samsung SM-S911B", DeviceProfile.deviceName("Samsung", "SM-S911B"))
    }

    @Test
    fun deviceName_fallsBackToAndroid() {
        assertEquals("Android", DeviceProfile.deviceName("  ", null))
    }

    @Test
    fun deviceName_truncatesToApiMaxLength() {
        val longModel = "X".repeat(200)
        val name = DeviceProfile.deviceName("Brand", longModel)
        assertEquals(DeviceProfile.DEVICE_NAME_MAX, name.length)
    }

    @Test
    fun userAgent_includesAppVersionPlatformAndSdk() {
        assertEquals(
            "LushAIEdu_PLS/1.0 (Android 15; Pixel 8; sdk 35)",
            DeviceProfile.userAgent(
                appVersion = "1.0",
                androidRelease = "15",
                model = "Pixel 8",
                sdkInt = 35,
            ),
        )
    }
}
