package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.UserSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class TeacherProfileSummaryTest {

    @Test
    fun profileSummary_joinsMultipleInstitutionsClassesAndSubjects() {
        val units = listOf(
            unit(
                className = "Class XI",
                subject = "Physics",
                institution = "School A",
                institutionId = "i1",
            ),
            unit(
                className = "Class XII",
                subject = "Chemistry",
                institution = "School B",
                institutionId = "i2",
            ),
            unit(
                className = "Class XI",
                subject = "Mathematics",
                institution = "School A",
                institutionId = "i1",
            ),
        )

        val summary = TeacherUiMappers.profileSummary(units)

        assertEquals("School A, School B", summary.institutionName)
        assertEquals("Class XI, Class XII", summary.className)
        assertEquals(listOf("Chemistry", "Mathematics", "Physics"), summary.subjects)
    }

    private fun unit(
        className: String,
        subject: String,
        institution: String,
        institutionId: String,
    ) = TeachingUnitOut(
        id = "u-$subject",
        class_id = "c-$className",
        subject_id = "s-$subject",
        class_name = className,
        subject_name = subject,
        teacher = teacher,
        status = TeachingUnitStatus.ACTIVE,
        institution_id = institutionId,
        institution_name = institution,
    )

    private val teacher = UserSummary(id = "t1", name = "Teacher")
}
