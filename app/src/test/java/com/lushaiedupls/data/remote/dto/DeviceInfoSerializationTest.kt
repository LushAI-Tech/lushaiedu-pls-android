package com.lushaiedupls.data.remote.dto

import com.lushaiedupls.data.remote.ApiClient
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoSerializationTest {

    private val json = ApiClient.json

    @Test
    fun deviceInfo_includesAllFieldsWhenFcmTokenIsPresent() {
        val encoded = json.encodeToString(
            DeviceInfo.serializer(),
            DeviceInfo(
                device_id = "install-1",
                platform = DevicePlatform.ANDROID,
                device_name = "Pixel 8",
                user_agent = "LushAIEdu_PLS/1.0 (Android 15; Pixel 8; sdk 35)",
                fcm_token = "fcm-abc",
            ),
        )
        val body = json.parseToJsonElement(encoded).jsonObject
        assertEquals("install-1", body["device_id"]!!.jsonPrimitive.content)
        assertEquals("ANDROID", body["platform"]!!.jsonPrimitive.content)
        assertEquals("Pixel 8", body["device_name"]!!.jsonPrimitive.content)
        assertEquals(
            "LushAIEdu_PLS/1.0 (Android 15; Pixel 8; sdk 35)",
            body["user_agent"]!!.jsonPrimitive.content,
        )
        assertEquals("fcm-abc", body["fcm_token"]!!.jsonPrimitive.content)
    }

    @Test
    fun loginRequest_includesDeviceFcmToken() {
        val encoded = json.encodeToString(
            LoginRequest.serializer(),
            LoginRequest(
                identifier = "student@lushaiedu.example.com",
                password = "Test@1234",
                device = DeviceInfo(
                    device_id = "install-1",
                    platform = DevicePlatform.ANDROID,
                    device_name = "sdk_gphone64_arm64",
                    user_agent = "LushAIEdu_PLS/1.0 (Android 16; sdk_gphone64_arm64; sdk 36)",
                    fcm_token = "fcm-abc",
                ),
            ),
        )
        val device = json.parseToJsonElement(encoded).jsonObject["device"]!!.jsonObject
        assertEquals("fcm-abc", device["fcm_token"]!!.jsonPrimitive.content)
    }

    @Test
    fun deviceInfo_omitsFcmTokenWhenMissingSoReloginDoesNotClearIt() {
        val encoded = json.encodeToString(
            DeviceInfo.serializer(),
            DeviceInfo(
                device_id = "install-1",
                platform = DevicePlatform.ANDROID,
                device_name = "Pixel 8",
                user_agent = "LushAIEdu_PLS/1.0 (Android 15; Pixel 8; sdk 35)",
                fcm_token = null,
            ),
        )
        val body = json.parseToJsonElement(encoded).jsonObject
        assertTrue(body.containsKey("device_id"))
        assertTrue(body.containsKey("platform"))
        assertTrue(body.containsKey("device_name"))
        assertTrue(body.containsKey("user_agent"))
        assertFalse(body.containsKey("fcm_token"))
    }
}
