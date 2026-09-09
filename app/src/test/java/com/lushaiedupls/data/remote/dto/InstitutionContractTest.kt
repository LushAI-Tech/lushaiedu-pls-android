package com.lushaiedupls.data.remote.dto

import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.ApiClient
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstitutionContractTest {

    private val json = ApiClient.json

    @Test
    fun studentOnboarding_encodesInstitutionWithClassAndSubjects() {
        val body = CompleteOnboardingRequest(
            role = UserRole.STUDENT,
            name = "Ava",
            institution_id = "inst-1",
            class_id = "class-1",
            subject_ids = listOf("sub-1", "sub-2"),
        )
        val encoded = json.encodeToString(CompleteOnboardingRequest.serializer(), body)
        val parsed = json.parseToJsonElement(encoded).jsonObject

        assertEquals("STUDENT", parsed["role"]!!.jsonPrimitive.content)
        assertEquals("inst-1", parsed["institution_id"]!!.jsonPrimitive.content)
        assertEquals("class-1", parsed["class_id"]!!.jsonPrimitive.content)
        assertTrue(parsed.containsKey("subject_ids"))
    }

    @Test
    fun classCreate_requiresInstitutionId() {
        val body = ClassCreate(
            institution_id = "inst-1",
            name = "Class X",
            sort_order = 1,
        )
        val encoded = json.encodeToString(ClassCreate.serializer(), body)
        val parsed = json.parseToJsonElement(encoded).jsonObject
        assertEquals("inst-1", parsed["institution_id"]!!.jsonPrimitive.content)
        assertEquals("Class X", parsed["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun periodCreate_requiresInstitutionId() {
        val body = PeriodCreate(
            institution_id = "inst-1",
            name = "Period 1",
            start_time = "09:00",
            end_time = "09:45",
        )
        val encoded = json.encodeToString(PeriodCreate.serializer(), body)
        val parsed = json.parseToJsonElement(encoded).jsonObject
        assertEquals("inst-1", parsed["institution_id"]!!.jsonPrimitive.content)
        assertFalse(parsed["institution_id"]!!.jsonPrimitive.content.isBlank())
    }

    @Test
    fun classOut_decodesInstitutionId() {
        val decoded = json.decodeFromString(
            ClassOut.serializer(),
            """{"id":"c1","name":"X","sort_order":1,"is_active":true,"institution_id":"inst-1"}""",
        )
        assertEquals("inst-1", decoded.institution_id)
        assertEquals("X", decoded.name)
    }

    @Test
    fun periodsForInstitution_keepsMatchingRows() {
        val periods = listOf(
            PeriodOut(
                id = "p1",
                name = "P1",
                start_time = "09:00",
                end_time = "09:45",
                sort_order = 1,
                is_active = true,
                institution_id = "inst-a",
            ),
            PeriodOut(
                id = "p2",
                name = "P2",
                start_time = "10:00",
                end_time = "10:45",
                sort_order = 2,
                is_active = true,
                institution_id = "inst-b",
            ),
        )
        val scoped = TeacherUiMappers.periodsForInstitution(periods, "inst-a")
        assertEquals(listOf("p1"), scoped.map { it.id })
    }

    @Test
    fun subjectCreate_requiresInstitutionAndClass() {
        val body = SubjectCreate(
            institution_id = "inst-1",
            class_id = "class-1",
            name = "Physics",
        )
        val encoded = json.encodeToString(SubjectCreate.serializer(), body)
        val parsed = json.parseToJsonElement(encoded).jsonObject

        assertEquals("inst-1", parsed["institution_id"]!!.jsonPrimitive.content)
        assertEquals("class-1", parsed["class_id"]!!.jsonPrimitive.content)
        assertEquals("Physics", parsed["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun teacherInstitutionAssignment_encodesInstitutionAndReplaceFlag() {
        val body = TeacherInstitutionAssignmentRequest(
            institution_id = "inst-1",
            assignments = listOf(
                TeacherAssignment(class_id = "class-1", subject_id = "sub-1"),
            ),
            replace_existing = false,
        )
        val encoded = json.encodeToString(TeacherInstitutionAssignmentRequest.serializer(), body)
        val parsed = json.parseToJsonElement(encoded).jsonObject

        assertEquals("inst-1", parsed["institution_id"]!!.jsonPrimitive.content)
        assertEquals("false", parsed["replace_existing"]!!.jsonPrimitive.content)
        assertTrue(parsed.containsKey("assignments"))
    }
}
