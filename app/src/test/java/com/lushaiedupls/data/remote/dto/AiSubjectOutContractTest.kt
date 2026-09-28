package com.lushaiedupls.data.remote.dto

import com.lushaiedupls.data.remote.ApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiSubjectOutContractTest {

    private val json = ApiClient.json

    @Test
    fun aiSubject_decodesInstitutionFields() {
        val decoded = json.decodeFromString(
            AiSubjectOut.serializer(),
            """
            {
              "subject_id": "sub-math-zion",
              "stem_subject_id": "stem-math",
              "name": "Math",
              "code": "MATH",
              "class_id": "cls-xi-zion",
              "class_name": "Class XI",
              "institution_id": "inst-zion",
              "institution_name": "Zion School"
            }
            """.trimIndent(),
        )

        assertEquals("sub-math-zion", decoded.subject_id)
        assertEquals("stem-math", decoded.stem_subject_id)
        assertEquals("inst-zion", decoded.institution_id)
        assertEquals("Zion School", decoded.institution_name)
        assertEquals("Class XI", decoded.class_name)
    }

    @Test
    fun aiSubject_toleratesMissingInstitutionFields() {
        val decoded = json.decodeFromString(
            AiSubjectOut.serializer(),
            """
            {
              "subject_id": "sub-math",
              "stem_subject_id": "stem-math",
              "name": "Math",
              "class_id": "cls-xi",
              "class_name": "Class XI"
            }
            """.trimIndent(),
        )

        assertNull(decoded.institution_id)
        assertNull(decoded.institution_name)
        assertEquals("Math", decoded.name)
    }
}
