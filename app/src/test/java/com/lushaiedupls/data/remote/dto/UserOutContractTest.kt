package com.lushaiedupls.data.remote.dto

import com.lushaiedupls.data.remote.ApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserOutContractTest {

    private val json = ApiClient.json

    @Test
    fun userOut_decodesProfileEnrollmentAndVerificationFields() {
        val decoded = json.decodeFromString(
            UserOut.serializer(),
            """
            {
              "id": "u1",
              "email": "parent@example.com",
              "name": "Parent User",
              "phone": "9876543210",
              "gender": "FEMALE",
              "role": "PARENT",
              "status": "ACTIVE",
              "onboarding_state": "COMPLETE",
              "class_name": null,
              "subjects": [],
              "institution_name": null,
              "created_at": "2026-08-01T00:00:00Z",
              "has_password": true,
              "email_verified": true
            }
            """.trimIndent(),
        )

        assertEquals(UserRole.PARENT, decoded.role)
        assertEquals(Gender.FEMALE, decoded.gender)
        assertTrue(decoded.email_verified)
        assertTrue(decoded.subjects.isEmpty())
    }

    @Test
    fun userOut_decodesStudentEnrollmentFields() {
        val decoded = json.decodeFromString(
            UserOut.serializer(),
            """
            {
              "id": "u2",
              "email": "student@example.com",
              "name": "Student User",
              "role": "STUDENT",
              "status": "ACTIVE",
              "onboarding_state": "COMPLETE",
              "class_name": "Class XII",
              "subjects": ["Physics", "Chemistry"],
              "institution_name": "Demo School",
              "created_at": "2026-08-01T00:00:00Z",
              "email_verified": false
            }
            """.trimIndent(),
        )

        assertEquals("Demo School", decoded.institution_name)
        assertEquals("Class XII", decoded.class_name)
        assertEquals(listOf("Physics", "Chemistry"), decoded.subjects)
    }
}
