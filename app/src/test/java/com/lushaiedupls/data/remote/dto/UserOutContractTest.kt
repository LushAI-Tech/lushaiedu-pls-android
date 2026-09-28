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
        assertTrue(decoded.teaching_institutions.isEmpty())
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
        assertTrue(decoded.teaching_institutions.isEmpty())
    }

    @Test
    fun userOut_decodesTeacherInstitutionsGroupedBySchool() {
        val decoded = json.decodeFromString(
            UserOut.serializer(),
            """
            {
              "id": "t1",
              "email": "teacher@example.com",
              "name": "Teacher User",
              "role": "TEACHER",
              "status": "ACTIVE",
              "onboarding_state": "COMPLETE",
              "class_id": null,
              "class_name": null,
              "subjects": [],
              "institution_id": null,
              "institution_name": null,
              "teaching_institutions": [
                {
                  "institution_id": "inst-1",
                  "institution_name": "Zion School",
                  "assignments": [
                    {
                      "teaching_unit_id": "tu1",
                      "class_id": "c1",
                      "class_name": "Class 8A",
                      "subject_id": "s1",
                      "subject_name": "Math"
                    },
                    {
                      "teaching_unit_id": "tu2",
                      "class_id": "c1",
                      "class_name": "Class 8A",
                      "subject_id": "s2",
                      "subject_name": "Science"
                    },
                    {
                      "teaching_unit_id": "tu3",
                      "class_id": "c2",
                      "class_name": "Class 9B",
                      "subject_id": "s3",
                      "subject_name": "English"
                    }
                  ]
                },
                {
                  "institution_id": "inst-2",
                  "institution_name": "Aizawl Public School",
                  "assignments": [
                    {
                      "teaching_unit_id": "tu4",
                      "class_id": "c3",
                      "class_name": "Class 6",
                      "subject_id": "s4",
                      "subject_name": "History"
                    }
                  ]
                }
              ],
              "created_at": "2026-08-01T00:00:00Z",
              "email_verified": true
            }
            """.trimIndent(),
        )

        assertEquals(UserRole.TEACHER, decoded.role)
        assertEquals(null, decoded.class_id)
        assertEquals(null, decoded.institution_id)
        assertTrue(decoded.subjects.isEmpty())
        assertEquals(2, decoded.teaching_institutions.size)
        val zion = decoded.teaching_institutions[0]
        assertEquals("inst-1", zion.institution_id)
        assertEquals("Zion School", zion.institution_name)
        assertEquals(3, zion.assignments.size)
        assertEquals("Class 8A", zion.assignments[0].class_name)
        assertEquals("Math", zion.assignments[0].subject_name)
        assertEquals("Class 8A", zion.assignments[1].class_name)
        assertEquals("Science", zion.assignments[1].subject_name)
        assertEquals("English", zion.assignments[2].subject_name)
        assertEquals("Aizawl Public School", decoded.teaching_institutions[1].institution_name)
        assertEquals("History", decoded.teaching_institutions[1].assignments[0].subject_name)
    }

    @Test
    fun userOut_teacherWithNoInstitutionsDecodesEmptyList() {
        val decoded = json.decodeFromString(
            UserOut.serializer(),
            """
            {
              "id": "t2",
              "email": "new@example.com",
              "name": "New Teacher",
              "role": "TEACHER",
              "status": "ACTIVE",
              "onboarding_state": "COMPLETE",
              "class_id": null,
              "class_name": null,
              "subjects": [],
              "institution_id": null,
              "institution_name": null,
              "teaching_institutions": [],
              "created_at": "2026-08-01T00:00:00Z",
              "email_verified": true
            }
            """.trimIndent(),
        )

        assertTrue(decoded.teaching_institutions.isEmpty())
        assertEquals(null, decoded.institution_id)
        assertEquals(null, decoded.class_id)
    }
}
