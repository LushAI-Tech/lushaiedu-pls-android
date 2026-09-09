package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableSubjectParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parse_subjectOutArray() {
        val element = json.parseToJsonElement(
            """
            [
              {
                "id": "sub-math",
                "class_id": "class-1",
                "name": "Mathematics",
                "sort_order": 1,
                "is_active": true
              }
            ]
            """.trimIndent(),
        )
        val parsed = TimetableSubjectParser.parse(element)
        assertEquals(1, parsed.size)
        assertEquals("sub-math", parsed[0].id)
        assertEquals("Mathematics", parsed[0].name)
    }

    @Test
    fun parse_teachingUnitShapedArray_usesSubjectName() {
        val element = json.parseToJsonElement(
            """
            [
              {
                "id": "unit-1",
                "class_id": "class-1",
                "subject_id": "sub-chem",
                "class_name": "Class XII",
                "subject_name": "Chemistry",
                "status": "ACTIVE"
              }
            ]
            """.trimIndent(),
        )
        val parsed = TimetableSubjectParser.parse(element)
        assertEquals("sub-chem", parsed[0].id)
        assertEquals("Chemistry", parsed[0].name)
    }

    @Test
    fun parse_wrappedItemsObject() {
        val element = json.parseToJsonElement(
            """{ "items": [{ "id": "sub-1", "name": "Physics", "is_active": true }] }""",
        )
        val parsed = TimetableSubjectParser.parse(element)
        assertEquals(listOf("Physics"), parsed.map { it.name })
    }

    @Test
    fun sessionOptions_mergesUnitsWhenApiNamesAreBlank() {
        val api = listOf(
            SubjectOut(id = "blank", class_id = "class-1", name = "", is_active = true),
        )
        val units = listOf(
            TeachingUnitOut(
                id = "u1",
                class_id = "class-1",
                subject_id = "sub-bio",
                class_name = "Class XII",
                subject_name = "Biology",
                status = TeachingUnitStatus.ACTIVE,
            ),
        )
        val options = TimetableSubjectParser.sessionOptions(api, units, classId = "class-1")
        assertTrue(options.any { it.name == "Biology" && it.id == "sub-bio" })
    }

    @Test
    fun sessionOptions_matchesUnitsByClassName() {
        val units = listOf(
            TeachingUnitOut(
                id = "u1",
                class_id = "other-id",
                subject_id = "sub-eng",
                class_name = "Class X",
                subject_name = "English",
                status = TeachingUnitStatus.ACTIVE,
            ),
        )
        val options = TimetableSubjectParser.sessionOptions(
            apiSubjects = emptyList(),
            units = units,
            classId = "listed-class-id",
            className = "Class X",
        )
        assertEquals(listOf("English"), options.map { it.name })
    }

    @Test
    fun subjectsFromUnits_onlyTeachersAssignedSubjectForClass() {
        val units = listOf(
            TeachingUnitOut(
                id = "u1",
                class_id = "class-1",
                subject_id = "sub-phy",
                class_name = "Class XII",
                subject_name = "Physics",
                status = TeachingUnitStatus.ACTIVE,
            ),
            TeachingUnitOut(
                id = "u2",
                class_id = "class-2",
                subject_id = "sub-chem",
                class_name = "Class XI",
                subject_name = "Chemistry",
                status = TeachingUnitStatus.ACTIVE,
            ),
        )
        val options = TimetableSubjectParser.subjectsFromUnits(
            units = units,
            classId = "class-1",
            className = "Class XII",
        )
        assertEquals(listOf("Physics"), options.map { it.name })
        assertEquals(listOf("sub-phy"), options.map { it.id })
    }
}
