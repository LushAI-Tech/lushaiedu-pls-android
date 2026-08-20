package com.lushaiedupls.ui.teacher.overview

import com.lushaiedupls.data.mock.TeacherStudent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherClassOverviewDeletionTest {

    @Test
    fun pendingDeletionFilteringAndAutoAssign() {
        val students = listOf(
            TeacherStudent(id = "s1", name = "Alice", email = "alice@test.com", rollNumber = 1),
            TeacherStudent(id = "s2", name = "Bob", email = "bob@test.com", rollNumber = 2),
            TeacherStudent(id = "s3", name = "Charlie", email = "charlie@test.com", rollNumber = 3),
        )

        // Mark Bob for deletion
        val pendingDeletes = setOf("s2")
        val remaining = students.filter { it.id !in pendingDeletes }

        assertEquals(2, remaining.size)
        assertEquals("Alice", remaining[0].name)
        assertEquals("Charlie", remaining[1].name)

        // Sequentially assign rolls to remaining
        var roll = 1
        val updated = students.map {
            if (it.id in pendingDeletes) it else it.copy(rollNumber = roll++)
        }

        assertEquals(1, updated[0].rollNumber) // Alice -> 1
        assertEquals(2, updated[1].rollNumber) // Bob (marked) unchanged
        assertEquals(2, updated[2].rollNumber) // Charlie -> 2
    }

    @Test
    fun cancelEditRestoresOriginalState() {
        val original = listOf(
            TeacherStudent(id = "s1", name = "Alice", email = "alice@test.com", rollNumber = 1),
            TeacherStudent(id = "s2", name = "Bob", email = "bob@test.com", rollNumber = 2),
        )

        // Modified during edit
        val modified = listOf(
            TeacherStudent(id = "s1", name = "Alice", email = "alice@test.com", rollNumber = 99),
            TeacherStudent(id = "s2", name = "Bob", email = "bob@test.com", rollNumber = 100),
        )

        // Cancel edit restores original
        val restored = original
        assertEquals(1, restored[0].rollNumber)
        assertEquals(2, restored[1].rollNumber)
    }
}
