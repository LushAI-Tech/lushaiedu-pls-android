package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.UserSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class StudentEnrollmentSummaryTest {

    @Test
    fun enrollmentSummary_collectsInstitutionClassAndSubjects() {
        val units = listOf(
            unit("Physics"),
            unit("Chemistry"),
        )

        val summary = StudentUiMappers.enrollmentSummary(units)

        assertEquals("Demo School", summary.institutionName)
        assertEquals("Class XII", summary.className)
        assertEquals(listOf("Chemistry", "Physics"), summary.subjects)
    }

    private fun unit(subject: String) = TeachingUnitOut(
        id = "u-$subject",
        class_id = "c1",
        subject_id = "s-$subject",
        class_name = "Class XII",
        subject_name = subject,
        teacher = teacher,
        status = TeachingUnitStatus.ACTIVE,
        institution_name = "Demo School",
    )

    private val teacher = UserSummary(id = "t1", name = "Teacher")
}
