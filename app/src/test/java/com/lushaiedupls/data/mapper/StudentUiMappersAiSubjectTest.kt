package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.AiSubjectOut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentUiMappersAiSubjectTest {

    @Test
    fun aiSubjects_mapsInstitutionAndClassTitle() {
        val mapped = StudentUiMappers.aiSubjects(
            listOf(
                AiSubjectOut(
                    subject_id = "sub-math-zion",
                    stem_subject_id = "stem-math",
                    name = "Math",
                    code = "MATH",
                    class_id = "cls-xi-zion",
                    class_name = "Class XI",
                    institution_id = "inst-zion",
                    institution_name = "Zion School",
                ),
            ),
        )
        val subject = mapped.single()
        assertEquals("sub-math-zion", subject.id)
        assertEquals("cls-xi-zion", subject.classId)
        assertEquals("Class XI", subject.className)
        assertEquals("Math", subject.name)
        assertEquals("inst-zion", subject.institutionId)
        assertEquals("Zion School", subject.institutionName)
    }

    @Test
    fun aiSubjects_toleratesMissingInstitutionFields() {
        val mapped = StudentUiMappers.aiSubjects(
            listOf(
                AiSubjectOut(
                    subject_id = "sub-math",
                    stem_subject_id = "stem-math",
                    name = "Math",
                    class_id = "cls-xi",
                    class_name = "XI",
                ),
            ),
        )
        val subject = mapped.single()
        assertEquals("", subject.institutionId)
        assertEquals("", subject.institutionName)
        assertEquals("Math", subject.name)
        assertTrue(StudentUiMappers.institutionOptions(mapped).isEmpty())
    }

    @Test
    fun mergeInstitutionOptions_keepsFirstNameAndDropsBlanks() {
        val merged = StudentUiMappers.mergeInstitutionOptions(
            listOf("inst-zion" to "Zion School"),
            listOf("inst-zion" to "", "inst-aps" to "Aizawl Public School"),
            listOf("" to "Ignored"),
        )
        assertEquals(
            listOf("inst-zion" to "Zion School", "inst-aps" to "Aizawl Public School"),
            merged,
        )
    }

    @Test
    fun queryInstitutionId_onlyWhenMultipleInstitutions() {
        assertNull(StudentUiMappers.queryInstitutionId(listOf("inst-1"), "inst-1"))
        assertEquals(
            "inst-2",
            StudentUiMappers.queryInstitutionId(listOf("inst-1", "inst-2"), "inst-2"),
        )
        assertNull(StudentUiMappers.queryInstitutionId(listOf("inst-1", "inst-2"), null))
    }
}
