package com.lushaiedupls.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSessionConflictTest {

    @Test
    fun parse_stringDetail_usesApiMessage() {
        val parsed = parseDeviceSessionConflict(
            """{"detail":"No login is allowed while another device is still active. Please sign out on that device first."}""",
        )
        assertEquals(
            "No login is allowed while another device is still active. Please sign out on that device first.",
            parsed?.message,
        )
        assertTrue(parsed?.devices.isNullOrEmpty())
    }

    @Test
    fun parse_devicesArray_mapsAccountAndDevice() {
        val parsed = parseDeviceSessionConflict(
            """
            {
              "detail":"No login is allowed while another device is still active.",
              "email":"student@lushaiedu.example.com",
              "devices":[
                {
                  "device_name":"Pixel 8",
                  "platform":"ANDROID",
                  "last_active_at":"15 Sep 2026, 18:20",
                  "email":"student@lushaiedu.example.com"
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals("student@lushaiedu.example.com", parsed?.accountLabel)
        assertEquals(1, parsed?.devices?.size)
        assertEquals("Pixel 8", parsed?.devices?.first()?.deviceName)
        assertEquals("Android", parsed?.devices?.first()?.platform)
    }

    @Test
    fun parse_detailObject_withNestedDevices() {
        val parsed = parseDeviceSessionConflict(
            """
            {
              "detail":{
                "message":"Device limit reached.",
                "devices":[
                  {"name":"iPhone","platform":"IOS","email":"a@example.com"},
                  {"device_name":"Web session","platform":"WEB","email":"b@example.com"}
                ]
              }
            }
            """.trimIndent(),
        )
        assertEquals("Device limit reached.", parsed?.message)
        assertEquals(2, parsed?.devices?.size)
        assertEquals("iOS", parsed?.devices?.first()?.platform)
        assertEquals("a@example.com", parsed?.devices?.first()?.accountLabel)
        assertEquals("b@example.com", parsed?.devices?.last()?.accountLabel)
    }

    @Test
    fun extractConflictToken_regexFallback() {
        assertEquals(
            "tok_from_regex",
            extractConflictToken("""{"detail":{"message":"blocked","conflict_token":"tok_from_regex"}}"""),
        )
    }

    @Test
    fun parse_detailObject_withConflictToken() {
        val parsed = parseDeviceSessionConflict(
            """
            {
              "detail":{
                "message":"No login is allowed while another device is still active. Please sign out on that device first.",
                "conflict_token":"ctok_abc123"
              }
            }
            """.trimIndent(),
        )
        assertEquals(
            "No login is allowed while another device is still active. Please sign out on that device first.",
            parsed?.message,
        )
        assertEquals("ctok_abc123", parsed?.conflictToken)
    }

    @Test
    fun conflictOrNull_usesScreenshotCopy() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"No login is allowed while another device is still active. Please sign out on that device first."}""",
        )
        assertTrue(result.isDeviceSessionConflict())
        assertFalse(result.isAccountAlreadyExists())
        assertEquals(
            "No login is allowed while another device is still active. Please sign out on that device first.",
            result.deviceSessionConflictOrNull()?.message,
        )
        assertNull(result.deviceSessionConflictOrNull()?.conflictToken)
    }

    @Test
    fun conflictOrNull_objectDetail_extractsMessageAndToken() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":{"message":"No login is allowed while another device is still active.","conflict_token":"tok_xyz"}}""",
        )
        assertTrue(result.isDeviceSessionConflict())
        assertFalse(result.isAccountAlreadyExists())
        val conflict = result.deviceSessionConflictOrNull()
        assertEquals("No login is allowed while another device is still active.", conflict?.message)
        assertEquals("tok_xyz", conflict?.conflictToken)
        assertEquals(
            "No login is allowed while another device is still active.\n\n$DEVICE_SESSION_HINT",
            result.loginUserMessage(),
        )
    }

    @Test
    fun conflictOrNull_nullForEmailAlreadyExists() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"Email already registered"}""",
        )
        assertNull(result.deviceSessionConflictOrNull())
    }
}
