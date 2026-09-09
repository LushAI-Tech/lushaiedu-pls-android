package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.RollStatus
import com.lushaiedupls.data.remote.dto.UserSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class TeacherUiMappersRollTest {

    @Test
    fun unassignedMembersGetSequentialRollNumbersFallback() {
        val members = listOf(
            MemberOut(
                id = "m1",
                student = UserSummary(id = "s1", name = "Charlie", email = "charlie@test.com"),
                roll_no = null,
                roll_status = RollStatus.UNASSIGNED,
                joined_at = "2026-01-01T00:00:00Z",
            ),
            MemberOut(
                id = "m2",
                student = UserSummary(id = "s2", name = "Alice", email = "alice@test.com"),
                roll_no = null,
                roll_status = RollStatus.UNASSIGNED,
                joined_at = "2026-01-01T00:00:00Z",
            ),
            MemberOut(
                id = "m3",
                student = UserSummary(id = "s3", name = "Bob", email = "bob@test.com"),
                roll_no = null,
                roll_status = RollStatus.UNASSIGNED,
                joined_at = "2026-01-01T00:00:00Z",
            ),
        )

        val students = TeacherUiMappers.students(members)
        assertEquals(3, students.size)
        // Sorted alphabetically by name when roll_no is null
        assertEquals("Alice", students[0].name)
        assertEquals(1, students[0].rollNumber)
        assertEquals("Bob", students[1].name)
        assertEquals(2, students[1].rollNumber)
        assertEquals("Charlie", students[2].name)
        assertEquals(3, students[2].rollNumber)
    }

    @Test
    fun assignedMembersRetainRollNumbersAndSortOrder() {
        val members = listOf(
            MemberOut(
                id = "m1",
                student = UserSummary(id = "s1", name = "Charlie", email = "charlie@test.com"),
                roll_no = 15,
                roll_status = RollStatus.APPROVED,
                joined_at = "2026-01-01T00:00:00Z",
            ),
            MemberOut(
                id = "m2",
                student = UserSummary(id = "s2", name = "Alice", email = "alice@test.com"),
                roll_no = 3,
                roll_status = RollStatus.APPROVED,
                joined_at = "2026-01-01T00:00:00Z",
            ),
        )

        val students = TeacherUiMappers.students(members)
        assertEquals(2, students.size)
        assertEquals("Alice", students[0].name)
        assertEquals(3, students[0].rollNumber)
        assertEquals("Charlie", students[1].name)
        assertEquals(15, students[1].rollNumber)
    }

    @Test
    fun resolveTeachingUnitId_matchesClassSubjectAndInstitution() {
        val units = listOf(
            teachingUnit(id = "u1", classId = "c1", subjectId = "s1", institutionId = "inst-a"),
            teachingUnit(id = "u2", classId = "c1", subjectId = "s2", institutionId = "inst-a"),
            teachingUnit(id = "u3", classId = "c1", subjectId = "s1", institutionId = "inst-b"),
        )
        assertEquals(
            "u1",
            TeacherUiMappers.resolveTeachingUnitId(units, "c1", "s1", "inst-a"),
        )
        assertEquals(
            "u3",
            TeacherUiMappers.resolveTeachingUnitId(units, "c1", "s1", "inst-b"),
        )
        assertEquals(null, TeacherUiMappers.resolveTeachingUnitId(units, "c1", "s9", "inst-a"))
    }

    private fun teachingUnit(
        id: String,
        classId: String,
        subjectId: String,
        institutionId: String,
    ) = com.lushaiedupls.data.remote.dto.TeachingUnitOut(
        id = id,
        class_id = classId,
        subject_id = subjectId,
        class_name = "X",
        subject_name = "Physics",
        institution_id = institutionId,
    )
}
